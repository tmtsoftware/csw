package csw.services.event.perf

import java.io.PrintStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import akka.testkit.typed.scaladsl
import csw.services.event.perf.EventUtils.{nanosToMicros, nanosToSeconds}
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

class ModelObsPerfTestMultiJvmNode1 extends ModelObsPerfTest
class ModelObsPerfTestMultiJvmNode2 extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode3  extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode4  extends ModelObsPerfTest
//class ModelObsPerfTestMultiJvmNode5  extends ModelObsPerfTest
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
    extends MultiNodeSpec(PerfMultiNodeConfig)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  private val testConfigs = new TestConfigs(system.settings.config)
  private val testWiring  = new TestWiring(system)

  override def initialParticipants: Int = roles.size

  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  var throughputPlots: PlotResult = PlotResult()
  var latencyPlots: LatencyPlots  = LatencyPlots()

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(roles.last) {
      println("================================ Throughput msgs/s ================================")
      throughputPlots.printTable()
      println()
      latencyPlots.printTable(system.name)
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
          val subscriber =
            new ModelObsSubscriber(s"${subSetting.key}-$subId", subSetting, rep, testWiring)
          val doneF = subscriber.startSubscription()
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
          new ModelObsPublisher(s"${pubSetting.key}-$pubId", pubSetting, testWiring)
            .startPublishingWithEventGenerator()
        }
      }

      enterBarrier("publishers-started")

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, Duration.Inf)
          subscriber.printResult()

          histogramPerNode.add(subscriber.histogram)
          eventsReceivedPerNode += subscriber.eventsReceived
          totalTimePerNode = Math.max(totalTimePerNode, subscriber.totalTime)
      }

      val byteBuffer: ByteBuffer = ByteBuffer.allocate(326942)
      histogramPerNode.encodeIntoByteBuffer(byteBuffer)

      val publisher = testWiring.publisher
      Await.result(
        publisher.publish(
          EventUtils.perfResultEvent(byteBuffer.array(), eventsReceivedPerNode / nanosToSeconds(totalTimePerNode))
        ),
        30.seconds
      )

      runOn(roles.last) {
        val aggregatedResult = completionProbe.expectMessageType[AggregatedResult](5.minute)
        latencyPlots = latencyPlots.copy(
          plot50 = latencyPlots.plot50.addAll(aggregatedResult.latencyPlots.plot50),
          plot90 = latencyPlots.plot90.addAll(aggregatedResult.latencyPlots.plot90),
          plot99 = latencyPlots.plot99.addAll(aggregatedResult.latencyPlots.plot99)
        )

        throughputPlots = throughputPlots.addAll(aggregatedResult.throughputPlots)
      }

      enterBarrier("done")
      rep.halt()
    }
  }

  private val scenarios = new ModelObsScenarios(testConfigs)

  test("Perf results must be great for model observatory use case") {
    runScenario(scenarios.idealMultiNodeModelObsScenario)
  }

}
