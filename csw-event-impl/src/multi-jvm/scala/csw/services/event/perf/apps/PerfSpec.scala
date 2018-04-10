package csw.services.event.perf.apps

import java.util.concurrent.{ExecutorService, Executors}

import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import csw.services.event.perf._
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object PerfSpec extends MultiNodeConfig {
  val first: RoleName  = role("first")
  val second: RoleName = role("second")

  val barrierTimeout: FiniteDuration = 5.minutes

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))
}

class PerfSpecMultiJvmNode1 extends PerfSpec
class PerfSpecMultiJvmNode2 extends PerfSpec

class PerfSpec
    extends MultiNodeSpec(PerfSpec)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  import PerfSpec._

  val totalMessagesFactor: Double = system.settings.config.getDouble("csw.test.EventThroughputSpec.totalMessagesFactor")
  val batching: Boolean           = system.settings.config.getBoolean("csw.test.EventThroughputSpec.batching")

  var throughputPlot = PlotResult()
  var latencyPlots   = LatencyPlots()

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
    runOn(first) {
//      println("============= Throughput Results in mb/s =============")
//      println(throughputPlot.csv(system.name))
//      println()
//      println("============= Latency Results in µs =============")
//      println(latencyPlots.plot50.csv(system.name + "50"))
//      println(latencyPlots.plot90.csv(system.name + "90"))
//      println(latencyPlots.plot99.csv(system.name + "99"))
    }
    multiNodeSpecAfterAll()
  }

  val scenarios = List(
    TestSettings(
      testName = "1-to-1",
      totalMessages = adjustedTotalMessages(5000),
      burstSize = 1000,
      payloadSize = 100,
      publisherSubscriberPairs = 1,
      batching,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-1-1k",
      totalMessages = adjustedTotalMessages(5000),
      burstSize = 1000,
      payloadSize = 1000,
      publisherSubscriberPairs = 1,
      batching,
      singlePublisher = false
    )
  )

  def testScenario(testSettings: TestSettings, benchmarkFileReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val subscriberName = testName + "-subscriber"

//    runPerfFlames(first, second)(delay = 5.seconds, time = 30.seconds)

    implicit val ec: ExecutionContext = system.dispatcher

    runOn(second) {
      val rep = reporter(testName)

      val subscribers = for (n ← 1 to publisherSubscriberPairs) yield {
        val id         = if (testSettings.singlePublisher) 1 else n
        val subscriber = new SimpleSubscriber(testSettings, rep, id, benchmarkFileReporter)
        (subscriber.startSubscription(), subscriber)
      }

      enterBarrier(subscriberName + "-started")

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, 5.minute)
          subscriber.printResult()
      }
      enterBarrier(testName + "-done")

      rep.halt()
    }

    runOn(first) {
      val noOfPublishers = if (testSettings.singlePublisher) 1 else publisherSubscriberPairs
      enterBarrier(subscriberName + "-started")

      val publishers = for (n ← 1 to noOfPublishers) yield {
        new SimplePublisher(testSettings, n).start()
      }

      Await.result(Future.sequence(publishers), 5.minute)
      enterBarrier(testName + "-done")
    }
    enterBarrier("after-" + testName)
  }

  val reporter = BenchmarkFileReporter("PerfSpec", system)
  for (s ← scenarios) {
    test(
      s"Perf must be great for ${s.testName}," + s"${if (s.batching) s"burst size = ${s.burstSize}, " else " "}payloadSize = ${s.payloadSize}"
    ) {
      testScenario(s, reporter)
    }
  }

}
