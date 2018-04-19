package csw.services.event.perf

import java.io.PrintStream
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.EventUtils.{nanosToMicros, nanosToSeconds}
import org.HdrHistogram.Histogram
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object EventServiceScalabilityTest extends MultiNodeConfig {

  val totalNumberOfNodes: Int =
    System.getProperty("akka.test.FanInThroughputSpec.nrOfNodes") match {
      case null  ⇒ 4
      case value ⇒ value.toInt
    }

  for (n ← 1 to totalNumberOfNodes) role("node-" + n)

  val barrierTimeout: FiniteDuration = 5.minutes

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))
}

class EventServiceScalabilityTestMultiJvmNode1 extends EventServiceScalabilityTest
class EventServiceScalabilityTestMultiJvmNode2 extends EventServiceScalabilityTest
class EventServiceScalabilityTestMultiJvmNode3 extends EventServiceScalabilityTest
class EventServiceScalabilityTestMultiJvmNode4 extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode5 extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode6 extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode7  extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode8  extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode9  extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode10 extends EventServiceScalabilityTest

class EventServiceScalabilityTest
    extends MultiNodeSpec(EventServiceScalabilityTest)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  import EventServiceScalabilityTest._

  val testConfigs = new TestConfigs(system.settings.config)
  import testConfigs._

  var throughputPlots: PlotResult = PlotResult()
  var latencyPlots: LatencyPlots  = LatencyPlots()

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  override def initialParticipants: Int = roles.size

  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
//    runOn(second) {
//      println("================================ Throughput msgs/s ================================")
//      throughputPlots.printTable()
//      println()
//      latencyPlots.printTable(system.name)
//    }
    multiNodeSpecAfterAll()
  }

  def testScenario(testSettings: TestSettings, benchmarkFileReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val subscriberName           = testName + "-subscriber"
    var aggregatedEventsReceived = 0L
    var totalTime                = 0L

    val publisherNodes  = roles.take(roles.size / 2)
    val subscriberNodes = roles.takeRight(roles.size / 2)

    implicit val ec: ExecutionContext = system.dispatcher

    val id = myself.name.split("-").tail.head.toInt

    val allPairs = (1 to publisherSubscriberPairs).grouped(publisherSubscriberPairs / (totalNumberOfNodes / 2)).toList

    runOn(subscriberNodes: _*) {
      val subIds                         = allPairs(id - totalNumberOfNodes / 2 - 1)
      val rep                            = reporter(testName)
      val aggregatedHistogram: Histogram = new Histogram(SECONDS.toNanos(10), 3)

      val subscribers = subIds.map { n ⇒
        val subscriber = new Subscriber(testSettings, testConfigs, rep, n, n)
        val doneF      = subscriber.startSubscription()
        (doneF, subscriber)
      }

      enterBarrier(subscriberName + "-started")

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, 5.minute)
          subscriber.printResult()

          aggregatedHistogram.add(subscriber.histogram)
          aggregatedEventsReceived += subscriber.eventsReceived
          totalTime = Math.max(totalTime, subscriber.totalTime)
      }

      aggregateResult(testSettings, aggregatedEventsReceived, totalTime, aggregatedHistogram)

      enterBarrier(testName + "-done")
      rep.halt()

    }

    runOn(publisherNodes: _*) {
      val pubIds = allPairs(id - 1)
      println(
        "================================================================================================================================================"
      )
      println(
        s"[$testName]: Starting benchmark with ${totalNumberOfNodes / 2} publishers & ${totalNumberOfNodes / 2} subscribers $totalTestMsgs messages with " +
        s"throttling of $elements msgs/${per.toSeconds}s " +
        s"and payload size $payloadSize bytes"
      )
      println(
        "================================================================================================================================================"
      )

      enterBarrier(subscriberName + "-started")

      pubIds.foreach(id ⇒ new Publisher(testSettings, testConfigs, id).startPublishing())

      enterBarrier(testName + "-done")
    }
    enterBarrier("after-" + testName)
  }

  private def aggregateResult(
      testSettings: TestSettings,
      aggregatedEventsReceived: Long,
      totalTime: Long,
      aggregatedHistogram: Histogram
  ): Unit = {
    import testSettings._
    val throughput = aggregatedEventsReceived / nanosToSeconds(totalTime)

    throughputPlots = throughputPlots.addAll(PlotResult().add(testName, throughput))

    def percentile(p: Double): Double = nanosToMicros(aggregatedHistogram.getValueAtPercentile(p))

    val latencyPlotsTmp = LatencyPlots(
      PlotResult().add(testName, percentile(50.0)),
      PlotResult().add(testName, percentile(90.0)),
      PlotResult().add(testName, percentile(99.0))
    )

    latencyPlots = latencyPlots.copy(
      plot50 = latencyPlots.plot50.addAll(latencyPlotsTmp.plot50),
      plot90 = latencyPlots.plot90.addAll(latencyPlotsTmp.plot90),
      plot99 = latencyPlots.plot99.addAll(latencyPlotsTmp.plot99)
    )

    aggregatedHistogram.outputPercentileDistribution(
      new PrintStream(BenchmarkFileReporter.apply(s"Aggregated-$testName", system, logSettings = false).fos),
      1000.0
    )
  }

  private val reporter  = BenchmarkFileReporter("PerfSpec", system)
  private val scenarios = new Scenarios(testConfigs)

  for (s ← scenarios.payload) {
    test(s"Perf results must be great for ${s.testName} with payloadSize = ${s.payloadSize}") {
      testScenario(s, reporter)
    }
  }

}
