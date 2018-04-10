package csw.services.event.perf

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor._
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.Messages.Init
import org.scalatest._

import scala.concurrent.duration._

object EventServicePerfSpec extends MultiNodeConfig {
  val first: RoleName  = role("first")
  val second: RoleName = role("second")

  val barrierTimeout: FiniteDuration = 5.minutes

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))

  sealed trait Target {
    def tell(msg: Any, sender: ActorRef): Unit
    def ref: ActorRef
  }

  final case class ActorRefTarget(override val ref: ActorRef) extends Target {
    override def tell(msg: Any, sender: ActorRef) = ref.tell(msg, sender)
  }
}

class EventServicePerfSpecMultiJvmNode1 extends EventServicePerfSpec
class EventServicePerfSpecMultiJvmNode2 extends EventServicePerfSpec

class EventServicePerfSpec
    extends MultiNodeSpec(EventServicePerfSpec)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  import EventServicePerfSpec._

  val totalMessagesFactor: Double = system.settings.config.getDouble("csw.test.EventThroughputSpec.totalMessagesFactor")
  val actorSelection: Boolean     = system.settings.config.getBoolean("csw.test.EventThroughputSpec.actor-selection")
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
      println("============= Throughput Results in mb/s =============")
      println(throughputPlot.csv(system.name))
      println()
      println("============= Latency Results in µs =============")
      println(latencyPlots.plot50.csv(system.name + "50"))
      println(latencyPlots.plot90.csv(system.name + "90"))
      println(latencyPlots.plot99.csv(system.name + "99"))
    }
    multiNodeSpecAfterAll()
  }

  def identifySubscriber(name: String, r: RoleName = second): Target = {
    val sel = system.actorSelection(node(r) / "user" / name)
    sel ! Identify(None)
    val ref = expectMsgType[ActorIdentity](10.seconds).ref.get
    ActorRefTarget(ref)
  }

  val scenarios = List(
    TestSettings(
      testName = "1-to-1",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 1000,
      payloadSize = 100,
      publisherSubscriberPairs = 1,
      batching,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-1-size-1k",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 1000,
      payloadSize = 1000,
      publisherSubscriberPairs = 1,
      batching,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-1-size-10k",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 1000,
      payloadSize = 10000,
      publisherSubscriberPairs = 1,
      batching,
      singlePublisher = false
    ),
    TestSettings(
      testName = "5-to-5",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 200,
      payloadSize = 100,
      publisherSubscriberPairs = 5,
      batching,
      singlePublisher = false
    ),
    TestSettings(
      testName = "10-to-10",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 100,
      payloadSize = 100,
      publisherSubscriberPairs = 10,
      batching,
      singlePublisher = false
    ),
    TestSettings(
      testName = "1-to-5",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 100,
      payloadSize = 100,
      publisherSubscriberPairs = 5,
      batching,
      singlePublisher = true
    ),
    TestSettings(
      testName = "1-to-10",
      totalMessages = adjustedTotalMessages(10000),
      burstSize = 100,
      payloadSize = 100,
      publisherSubscriberPairs = 10,
      batching,
      singlePublisher = true
    )
  )

  def testScenario(testSettings: TestSettings, benchmarkFileReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val subscriberName = testName + "-subscriber"

    runPerfFlames(first, second)(delay = 5.seconds, time = 15.seconds)

    runOn(second) {
      val rep = reporter(testName)
      val subscribers = (1 to publisherSubscriberPairs).map { n ⇒
        val (noOfPublishers, id) = if (testSettings.singlePublisher) (1, 1) else (publisherSubscriberPairs, n)
        system.actorOf(
          SubscribingActor.props(rep, payloadSize, noOfPublishers, id),
          subscriberName + n
        )
      }
      enterBarrier(subscriberName + "-started")
      enterBarrier(testName + "-done")
      subscribers.foreach(_ ! PoisonPill)
      rep.halt()
    }

    runOn(first) {
      val noOfPublishers = if (testSettings.singlePublisher) 1 else publisherSubscriberPairs
      enterBarrier(subscriberName + "-started")
      val subscribers = (for (n ← 1 to publisherSubscriberPairs) yield identifySubscriber(subscriberName + n)).toArray
      val publishers = for (n ← 1 to noOfPublishers) yield {
        val subscriber = subscribers(n - 1)

        val throughputPlotProbe = TestProbe()
        val latencyPlotProbe    = TestProbe()
        val snd = system.actorOf(
          PublishingActor.props(
            subscriber,
            subscribers,
            testSettings,
            throughputPlotProbe.ref,
            latencyPlotProbe.ref,
            benchmarkFileReporter
          ),
          testName + "-publisher" + n
        )
        val terminationProbe = TestProbe()
        terminationProbe.watch(snd)
        snd ! Init
        (snd, terminationProbe, throughputPlotProbe, latencyPlotProbe)
      }
      publishers.foreach {
        case (snd, terminationProbe, throughputPlotProbe, latencyPlotProbe) ⇒
          terminationProbe.expectTerminated(snd, barrierTimeout)
          if (snd == publishers.head._1) {
            val throughputPlotResult = throughputPlotProbe.expectMsgType[PlotResult]
            val latencyPlotResult    = latencyPlotProbe.expectMsgType[LatencyPlots]
            throughputPlot = throughputPlot.addAll(throughputPlotResult)
            latencyPlots = latencyPlots.copy(
              plot50 = latencyPlots.plot50.addAll(latencyPlotResult.plot50),
              plot90 = latencyPlots.plot90.addAll(latencyPlotResult.plot90),
              plot99 = latencyPlots.plot99.addAll(latencyPlotResult.plot99)
            )
          }
      }
      enterBarrier(testName + "-done")
    }
    enterBarrier("after-" + testName)
  }

  val reporter = BenchmarkFileReporter("EventServicePerfSpec", system)
  for (s ← scenarios) {
    test(
      s"Perf must be great for ${s.testName}," + s"${if (s.batching) s"burst size = ${s.burstSize}, " else " "}payloadSize = ${s.payloadSize}"
    ) {
      testScenario(s, reporter)
    }
  }

}
