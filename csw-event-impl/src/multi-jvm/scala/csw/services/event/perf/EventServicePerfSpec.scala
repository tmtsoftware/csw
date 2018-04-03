package csw.services.event.perf

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor._
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import akka.testkit._
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.event.perf.Messages.Init
import csw.services.event.perf.testkit.{PerfFlamesSupport, RemotingMultiNodeSpec}
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.duration._

object EventServicePerfSpec extends MultiNodeConfig {
  val first: RoleName  = role("first")
  val second: RoleName = role("second")

  val barrierTimeout: FiniteDuration = 5.minutes

  val cfg: Config = ConfigFactory.parseString(s"""
       |include "logging.conf"
       |
       |csw.test.EventThroughputSpec {
       |# for serious measurements you should increase the totalMessagesFactor (20)
       |  totalMessagesFactor = 10.0
       |  actor-selection = off
       |  batching = on
       |
       |  throttling {
       |    elements = 300
       |    per = 1 second
       |  }
       |}
       |
       |akka {
       |  log-dead-letters = 100
       |  testconductor.barrier-timeout = 300s
       |  actor {
       |    provider = remote
       |    serialize-creators = false
       |    serialize-messages = false
       |
       |    serializers {
       |      kryo = "com.twitter.chill.akka.AkkaSerializer"
       |    }
       |    serialization-bindings {
       |      "csw.messages.TMTSerializable" = kryo
       |    }
       |  }
       |}
       |akka.remote.default-remote-dispatcher {
       |  fork-join-executor {
       |    # parallelism-factor = 0.5
       |    parallelism-min = 4
       |    parallelism-max = 4
       |  }
       |  # Set to 10 by default. Might be worthwhile to experiment with.
       |  # throughput = 100
       |}
     """.stripMargin)

  commonConfig(debugConfig(on = false).withFallback(cfg).withFallback(RemotingMultiNodeSpec.commonConfig))

  sealed trait Target {
    def tell(msg: Any, sender: ActorRef): Unit
    def ref: ActorRef
  }

  final case class ActorRefTarget(override val ref: ActorRef) extends Target {
    override def tell(msg: Any, sender: ActorRef) = ref.tell(msg, sender)
  }

  final case class ActorSelectionTarget(sel: ActorSelection, override val ref: ActorRef) extends Target {
    override def tell(msg: Any, sender: ActorRef) = sel.tell(msg, sender)
  }
}

class EventServicePerfSpecMultiJvmNode1 extends EventServicePerfSpec
class EventServicePerfSpecMultiJvmNode2 extends EventServicePerfSpec

abstract class EventServicePerfSpec extends RemotingMultiNodeSpec(EventServicePerfSpec) with PerfFlamesSupport {

  import EventServicePerfSpec._

  val totalMessagesFactor: Double = system.settings.config.getDouble("csw.test.EventThroughputSpec.totalMessagesFactor")
  val actorSelection: Boolean     = system.settings.config.getBoolean("csw.test.EventThroughputSpec.actor-selection")
  val batching: Boolean           = system.settings.config.getBoolean("csw.test.EventThroughputSpec.batching")

  var throughputPlot = PlotResult()
  var latencyPlots   = LatencyPlots()

  LoggingSystemFactory.start("perf", "", "", system)

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  override def initialParticipants: Int = roles.size

  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

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
    super.afterAll()
  }

  def identifyReceiver(name: String, r: RoleName = second): Target = {
    val sel = system.actorSelection(node(r) / "user" / name)
    sel ! Identify(None)
    val ref = expectMsgType[ActorIdentity](10.seconds).ref.get
    if (actorSelection) ActorSelectionTarget(sel, ref)
    else ActorRefTarget(ref)
  }

  val scenarios = List(
    TestSettings(testName = "1-to-1",
                 totalMessages = adjustedTotalMessages(20000),
                 burstSize = 1000,
                 payloadSize = 100,
                 senderReceiverPairs = 1,
                 batching),
    TestSettings(testName = "1-to-1-size-1k",
                 totalMessages = adjustedTotalMessages(20000),
                 burstSize = 1000,
                 payloadSize = 1000,
                 senderReceiverPairs = 1,
                 batching),
    TestSettings(testName = "1-to-1-size-10k",
                 totalMessages = adjustedTotalMessages(20000),
                 burstSize = 1000,
                 payloadSize = 10000,
                 senderReceiverPairs = 1,
                 batching),
    TestSettings(testName = "5-to-5",
                 totalMessages = adjustedTotalMessages(10000),
                 burstSize = 200,
                 payloadSize = 100,
                 senderReceiverPairs = 5,
                 batching),
    TestSettings(testName = "10-to-10",
                 totalMessages = adjustedTotalMessages(10000),
                 burstSize = 100,
                 payloadSize = 100,
                 senderReceiverPairs = 10,
                 batching)
  )

  def test(testSettings: TestSettings, benchmarkFileReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val receiverName = testName + "-subscriber"

    runPerfFlames(first, second)(delay = 5.seconds, time = 15.seconds)

    runOn(second) {
      val rep = reporter(testName)
      val receivers = (1 to senderReceiverPairs).map { n ⇒
        system.actorOf(
          SubscribingActor.props(rep, payloadSize, printTaskRunnerMetrics = n == 1, senderReceiverPairs, n),
          receiverName + n
        )
      }
      enterBarrier(receiverName + "-started")
      enterBarrier(testName + "-done")
      receivers.foreach(_ ! PoisonPill)
      rep.halt()
    }

    runOn(first) {
      enterBarrier(receiverName + "-started")
      val receivers = (for (n ← 1 to senderReceiverPairs) yield identifyReceiver(receiverName + n)).toArray
      val senders = for (n ← 1 to senderReceiverPairs) yield {
        val receiver = receivers(n - 1)

        val throughputPlotProbe = TestProbe()
        val latencyPlotProbe    = TestProbe()
        val snd = system.actorOf(
          PublishingActor.props(
            receiver,
            receivers,
            testSettings,
            throughputPlotProbe.ref,
            latencyPlotProbe.ref,
            printTaskRunnerMetrics = n == 1,
            benchmarkFileReporter
          ),
          testName + "-publisher" + n
        )
        val terminationProbe = TestProbe()
        terminationProbe.watch(snd)
        snd ! Init
        (snd, terminationProbe, throughputPlotProbe, latencyPlotProbe)
      }
      senders.foreach {
        case (snd, terminationProbe, throughputPlotProbe, latencyPlotProbe) ⇒
          terminationProbe.expectTerminated(snd, barrierTimeout)
          if (snd == senders.head._1) {
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

  "Max throughput of Event Service" must {
    val reporter = BenchmarkFileReporter("EventServicePerfSpec", system)
    for (s ← scenarios) {
      s"be great for ${s.testName}, burstSize = ${s.burstSize}, payloadSize = ${s.payloadSize}" in test(s, reporter)
    }
  }
}
