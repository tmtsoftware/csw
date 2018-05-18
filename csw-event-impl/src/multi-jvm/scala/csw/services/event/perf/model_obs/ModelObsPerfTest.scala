package csw.services.event.perf.model_obs

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import akka.testkit.typed.scaladsl
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.reporter._
import csw.services.event.perf.utils.EventUtils.nanosToSeconds
import csw.services.event.perf.utils.{EventUtils, PerfFlamesSupport}
import csw.services.event.perf.wiring.{TestConfigs, TestWiring}
import csw.services.event.scaladsl.EventPublisher
import org.HdrHistogram.Histogram
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationLong}

sealed trait BaseSetting {
  def subsystem: String
  def totalTestMsgs: Long
  def rate: Int
  def payloadSize: Int

  val warmup: Int = rate * 20
  val key         = s"$subsystem-${rate}Hz"
}

case class PubSetting(subsystem: String, noOfPubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
case class SubSetting(subsystem: String, noOfSubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting

case class JvmSetting(name: String, pubSettings: List[PubSetting], subSettings: List[SubSetting])

case class ModelObservatoryTestSettings(jvmSettings: List[JvmSetting])

object ModelObsMultiNodeConfig extends MultiNodeConfig {

  val totalNumberOfNodes: Int =
    System.getProperty("csw.event.perf.model-obs.nodes") match {
      case null  ⇒ 5
      case value ⇒ value.toInt
    }

  for (n ← 1 to totalNumberOfNodes) role("node-" + n)

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))

}

class ModelObsPerfTestMultiJvmNode1 extends ModelObsPerfTest
class ModelObsPerfTestMultiJvmNode2 extends ModelObsPerfTest
class ModelObsPerfTestMultiJvmNode3 extends ModelObsPerfTest
class ModelObsPerfTestMultiJvmNode4 extends ModelObsPerfTest
class ModelObsPerfTestMultiJvmNode5 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode6  extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode7  extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode8  extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode9  extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode10 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode11 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode12 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode13 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode14 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode15 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode16 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode17 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode18 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode19 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode20 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode21 extends ModelObsPerfTest

class ModelObsPerfTest
    extends MultiNodeSpec(ModelObsMultiNodeConfig)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  private val testConfigs = new TestConfigs(system.settings.config)
  private val testWiring  = new TestWiring(system)

  import testWiring._

  lazy val sharedPublisher: EventPublisher   = publisher
  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)

  var throughputPlots: PlotResult        = PlotResult()
  var latencyPlots: LatencyPlots         = LatencyPlots()
  var totalDropped: Map[String, Long]    = Map.empty
  var outOfOrderCount: Map[String, Long] = Map.empty

  override def initialParticipants: Int = roles.size

  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(roles.last) {
      throughputPlots.printTable()
      latencyPlots.printTable()
      printTotalDropped()
      printTotalOutOfOrderCount()
    }
    multiNodeSpecAfterAll()
  }

  def runScenario(testSettings: ModelObservatoryTestSettings): Unit = {
    val nodeId = myself.name.split("-").tail.head.toInt

    runOn(roles: _*) {
      val jvmSetting = testSettings.jvmSettings(nodeId - 1)
      import jvmSetting._

      val rep = reporter(s"ModelObsTest-$nodeId")

      val subscribers = subSettings.flatMap { subSetting ⇒
        import subSetting._
        (1 to noOfSubs).map { subId ⇒
          val subscriber = new ModelObsSubscriber(s"${subSetting.key}-$subId", subSetting, rep, testWiring)
          val doneF      = subscriber.startSubscription()
          (doneF, subscriber)
        }
      }

      val completionProbe = scaladsl.TestProbe[AggregatedResult]()(system.toTyped)

      var totalTimePerNode            = 0L
      var eventsReceivedPerNode       = 0L
      val histogramPerNode: Histogram = new Histogram(SECONDS.toNanos(10), 3)

      runOn(roles.last) {
        val resultAggregator = new ResultAggregator("ModelObsPerfTest", testWiring.subscriber, roles.size, completionProbe.ref)
        Await.result(resultAggregator.startSubscription().ready(), 30.seconds)
      }

      enterBarrier("subscribers-started")

      pubSettings.foreach { pubSetting ⇒
        import pubSetting._
        (1 to noOfPubs).foreach { pubId ⇒
          new ModelObsPublisher(s"${pubSetting.key}-$pubId", pubSetting, testConfigs, testWiring, sharedPublisher)
            .startPublishingWithEventGenerator()
        }
      }

      enterBarrier("publishers-started")

      var totalDroppedPerSubscriber: Long    = 0L
      var outOfOrderCountPerSubscriber: Long = 0L

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, Duration.Inf)
          subscriber.printResult()

          totalDroppedPerSubscriber += subscriber.totalDropped()
          outOfOrderCountPerSubscriber += subscriber.outOfOrderCount

          histogramPerNode.add(subscriber.histogram)
          eventsReceivedPerNode += subscriber.eventsReceived
          totalTimePerNode = Math.max(totalTimePerNode, subscriber.totalTime)
      }

      val byteBuffer: ByteBuffer = ByteBuffer.allocate(326942)
      histogramPerNode.encodeIntoByteBuffer(byteBuffer)

      val publisher = testWiring.publisher
      Await.result(
        publisher.publish(
          EventUtils.perfResultEvent(
            byteBuffer.array(),
            eventsReceivedPerNode / nanosToSeconds(totalTimePerNode),
            totalDroppedPerSubscriber,
            outOfOrderCountPerSubscriber
          )
        ),
        30.seconds
      )

      runOn(roles.last) {
        completionProbe.expectMessageType[AggregatedResult](5.minute)
      }

      enterBarrier("done")
      rep.halt()
    }
  }

  private def printTotalOutOfOrderCount(): Unit = {
    println("================================ Out of order ================================")
    outOfOrderCount.foreach {
      case (testName, outOfOrder) ⇒ println(s"$testName: ${if (testName.length < 7) "\t\t" else "\t"} $outOfOrder")
    }
  }

  private def printTotalDropped(): Unit = {
    println("================================ Total dropped ================================")
    totalDropped.foreach {
      case (testName, totalDroppedCount) ⇒ println(s"$testName: ${if (testName.length < 7) "\t\t" else "\t"} $totalDroppedCount")
    }
  }

  private val scenarios = new ModelObsScenarios(testConfigs)

  test("Perf results must be great for model observatory use case") {
    runScenario(scenarios.modelObsScenarioWithFiveProcesses)
  }

}
