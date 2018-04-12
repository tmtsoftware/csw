package csw.services.event.perf

import java.io.PrintStream
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.EventUtils.{nanosToMicros, nanosToSeconds}
import org.HdrHistogram.Histogram
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object EventServicePerfTest extends MultiNodeConfig {
  val first: RoleName  = role("first")
  val second: RoleName = role("second")

  val barrierTimeout: FiniteDuration = 5.minutes

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))
}

class EventServicePerfTestMultiJvmNode1 extends EventServicePerfTest
class EventServicePerfTestMultiJvmNode2 extends EventServicePerfTest

class EventServicePerfTest
    extends MultiNodeSpec(EventServicePerfTest)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  import EventServicePerfTest._

  val testConfigs = new TestConfigs(system.settings.config)
  import testConfigs._

  var throughputPlots: PlotResult = PlotResult()
  var latencyPlots: LatencyPlots  = LatencyPlots()
  var totalTime                   = 0L

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
    runOn(second) {
      println("================================ Throughput msgs/s ================================")
      throughputPlots.printTable()
      println()
      latencyPlots.printTable(system.name)
    }
    multiNodeSpecAfterAll()
  }

  val scenarios = List(
    TestSettings(
      testName = "1-to-1",
      totalMessages = adjustedTotalMessages(10000),
      payloadSize = 100,
      publisherSubscriberPairs = 1,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-1-size-1k",
      totalMessages = adjustedTotalMessages(10000),
      payloadSize = 1000,
      publisherSubscriberPairs = 1,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-1-size-10k",
      totalMessages = adjustedTotalMessages(10000),
      payloadSize = 10000,
      publisherSubscriberPairs = 1,
      singlePublisher = false
    ),
    TestSettings(
      testName = "5-to-5",
      totalMessages = adjustedTotalMessages(5000),
      payloadSize = 100,
      publisherSubscriberPairs = 5,
      singlePublisher = false
    ),
    TestSettings(
      testName = "10-to-10",
      totalMessages = adjustedTotalMessages(5000),
      payloadSize = 100,
      publisherSubscriberPairs = 10,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-5",
      totalMessages = adjustedTotalMessages(5000),
      payloadSize = 100,
      publisherSubscriberPairs = 5,
      singlePublisher = true
    ),
    TestSettings(
      testName = "1-to-10",
      totalMessages = adjustedTotalMessages(5000),
      payloadSize = 100,
      publisherSubscriberPairs = 10,
      singlePublisher = true
    )
  )

  def testScenario(testSettings: TestSettings, benchmarkFileReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val subscriberName           = testName + "-subscriber"
    var aggregatedEventsReceived = 0L

    runPerfFlames(first, second)(delay = 5.seconds, time = 40.seconds)

    implicit val ec: ExecutionContext = system.dispatcher

    runOn(second) {
      val rep                            = reporter(testName)
      val aggregatedHistogram: Histogram = new Histogram(SECONDS.toNanos(10), 3)

      val subscribers = for (n ← 1 to publisherSubscriberPairs) yield {
        val id         = if (testSettings.singlePublisher) 1 else n
        val subscriber = new Subscriber(testSettings, rep, id)
        (subscriber.startSubscription(), subscriber)
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

      aggregateResult(testSettings, aggregatedEventsReceived, aggregatedHistogram)
      enterBarrier(testName + "-done")

      rep.halt()
    }

    runOn(first) {
      val noOfPublishers = if (testSettings.singlePublisher) 1 else publisherSubscriberPairs
      enterBarrier(subscriberName + "-started")

      println("=============================================================================================================")
      println(
        s"[$testName]: Starting benchmark with $noOfPublishers publishers & $publisherSubscriberPairs subscribers $totalMessages messages with " +
        s"throttling of $throttlingElements msgs/${throttlingDuration.toSeconds}s " +
        s"and payload size $payloadSize"
      )
      println("=============================================================================================================")

      val publishers = for (n ← 1 to noOfPublishers) yield {
        new Publisher(testSettings, testConfigs, n).start()
      }

      Await.result(Future.sequence(publishers), 5.minute)
      enterBarrier(testName + "-done")
    }
    enterBarrier("after-" + testName)
  }

  private def aggregateResult(
      testSettings: TestSettings,
      aggregatedEventsReceived: Long,
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

//    println(s"Histogram of latencies in microseconds (µs) [${self.path.name}].")
    aggregatedHistogram.outputPercentileDistribution(
      new PrintStream(BenchmarkFileReporter.apply(s"PerfSpec", system, logSettings = false).fos),
      1000.0
    )
  }

  val reporter = BenchmarkFileReporter("PerfSpec", system)
  for (s ← scenarios) {
    test(s"Perf results must be great for ${s.testName} with payloadSize = ${s.payloadSize}") {
      testScenario(s, reporter)
    }
  }

}
