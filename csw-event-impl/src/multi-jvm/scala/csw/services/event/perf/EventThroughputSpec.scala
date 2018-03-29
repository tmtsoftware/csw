package csw.services.event.perf

import java.nio.ByteBuffer
import java.util.concurrent.Executors

import akka.actor._
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import akka.serialization.{ByteBufferSerializer, SerializerWithStringManifest}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.Messages.{FlowControl, Init}
import csw.services.logging.appenders.StdOutAppender
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.duration._

object MaxThroughputSpec extends MultiNodeConfig {
  val first  = role("first")
  val second = role("second")

  val barrierTimeout = 5.minutes

  val cfg = ConfigFactory.parseString(s"""
     include "logging.conf"

     # for serious measurements you should increase the totalMessagesFactor (20)
     akka.test.MaxThroughputSpec.totalMessagesFactor = 1.0
     akka.test.MaxThroughputSpec.real-message = on
     akka.test.MaxThroughputSpec.actor-selection = off
     akka {
       loglevel = INFO
       log-dead-letters = 100
       # avoid TestEventListener
       testconductor.barrier-timeout = ${barrierTimeout.toSeconds}s
       actor {
         provider = remote
         serialize-creators = false
         serialize-messages = false
         allow-java-serialization = on

         serializers {
           test = "csw.services.event.perf.MaxThroughputSpec$$TestSerializer"
           kryo = "com.twitter.chill.akka.AkkaSerializer"
         }
         serialization-bindings {
           "csw.services.event.perf.Messages$$FlowControl" = test
           "csw.messages.TMTSerializable" = kryo
         }
       }
       remote.artery {
         enabled = off

         # for serious measurements when running this test on only one machine
         # it is recommended to use external media driver
         # See akka-remote/src/test/resources/aeron.properties
         # advanced.embedded-media-driver = off
         # advanced.aeron-dir = "target/aeron"
         # on linux, use directory on ram disk, instead
         # advanced.aeron-dir = "/dev/shm/aeron"

         advanced.compression {
           actor-refs.advertisement-interval = 2 second
           manifests.advertisement-interval = 2 second
         }

         advanced {
           # inbound-lanes = 1
           # buffer-pool-size = 512
         }
       }
     }
     akka.remote.default-remote-dispatcher {
       fork-join-executor {
         # parallelism-factor = 0.5
         parallelism-min = 4
         parallelism-max = 4
       }
       # Set to 10 by default. Might be worthwhile to experiment with.
       # throughput = 100
     }

     """)

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

  class TestSerializer(val system: ExtendedActorSystem) extends SerializerWithStringManifest with ByteBufferSerializer {

    val FlowControlManifest = "A"

    override val identifier: Int = 100

    override def manifest(o: AnyRef): String =
      o match {
        case _: FlowControl ⇒ FlowControlManifest
      }

    override def toBinary(o: AnyRef, buf: ByteBuffer): Unit =
      o match {
        case FlowControl(id, burstStartTime) ⇒
          buf.putInt(id)
          buf.putLong(burstStartTime)
      }

    override def fromBinary(buf: ByteBuffer, manifest: String): AnyRef =
      manifest match {
        case FlowControlManifest ⇒ FlowControl(buf.getInt, buf.getLong)
      }

    override def toBinary(o: AnyRef): Array[Byte] = o match {
      case FlowControl(id, burstStartTime) ⇒
        val buf = ByteBuffer.allocate(12)
        toBinary(o, buf)
        buf.flip()
        val bytes = new Array[Byte](buf.remaining)
        buf.get(bytes)
        bytes
    }

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
      fromBinary(ByteBuffer.wrap(bytes), manifest)
  }

}

class MaxThroughputSpecMultiJvmNode1 extends MaxThroughputSpec
class MaxThroughputSpecMultiJvmNode2 extends MaxThroughputSpec

abstract class MaxThroughputSpec extends RemotingMultiNodeSpec(MaxThroughputSpec) {

  import MaxThroughputSpec._

  val totalMessagesFactor = system.settings.config.getDouble("akka.test.MaxThroughputSpec.totalMessagesFactor")
  val realMessage         = system.settings.config.getBoolean("akka.test.MaxThroughputSpec.real-message")
  val actorSelection      = system.settings.config.getBoolean("akka.test.MaxThroughputSpec.actor-selection")

  var plot = PlotResult()

  LoggingSystemFactory.start("", "", "", system).setAppenders(List(StdOutAppender))

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  override def initialParticipants = roles.size

  lazy val reporterExecutor = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(first) {
      println(plot.csv(system.name))
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
    TestSettings(testName = "warmup",
                 totalMessages = adjustedTotalMessages(10000),
                 burstSize = 1000,
                 payloadSize = 100,
                 senderReceiverPairs = 1,
                 realMessage),
    TestSettings(testName = "1-to-1",
                 totalMessages = adjustedTotalMessages(50000),
                 burstSize = 1000,
                 payloadSize = 100,
                 senderReceiverPairs = 1,
                 realMessage),
    TestSettings(testName = "1-to-1-size-1k",
                 totalMessages = adjustedTotalMessages(20000),
                 burstSize = 1000,
                 payloadSize = 1000,
                 senderReceiverPairs = 1,
                 realMessage),
    TestSettings(testName = "1-to-1-size-10k",
                 totalMessages = adjustedTotalMessages(5000),
                 burstSize = 1000,
                 payloadSize = 10000,
                 senderReceiverPairs = 1,
                 realMessage),
    TestSettings(
      testName = "5-to-5",
      totalMessages = adjustedTotalMessages(20000),
      burstSize = 200, /* don't exceed the send queue capacity 200*5*3=3000*/
      payloadSize = 100,
      senderReceiverPairs = 5,
      realMessage
    )
  )

  def test(testSettings: TestSettings, resultReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val receiverName = testName + "-rcv"

//    runPerfFlames(first, second)(delay = 5.seconds, time = 15.seconds)

    runOn(second) {
      val rep = reporter(testName)
      val receivers = (1 to senderReceiverPairs).map { n ⇒
        system.actorOf(
          SubscribingActor.props(rep, payloadSize, printTaskRunnerMetrics = n == 1, senderReceiverPairs),
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
      val ignore    = TestProbe()
      val receivers = (for (n ← 1 to senderReceiverPairs) yield identifyReceiver(receiverName + n)).toArray
      val senders = for (n ← 1 to senderReceiverPairs) yield {
        val receiver = receivers(n - 1)

        val plotProbe = TestProbe()
        val snd = system.actorOf(
          PublishingActor.props(
            receiver,
            receivers,
            testSettings,
            plotProbe.ref,
            printTaskRunnerMetrics = n == 1,
            resultReporter
          ),
          testName + "-snd" + n
        )
        val terminationProbe = TestProbe()
        terminationProbe.watch(snd)
        snd ! Init
        (snd, terminationProbe, plotProbe)
      }
      senders.foreach {
        case (snd, terminationProbe, plotProbe) ⇒
          terminationProbe.expectTerminated(snd, barrierTimeout)
          if (snd == senders.head._1) {
            val plotResult = plotProbe.expectMsgType[PlotResult]
            plot = plot.addAll(plotResult)
          }
      }
      enterBarrier(testName + "-done")
    }

    enterBarrier("after-" + testName)
  }

  "Max throughput of Artery" must {
    val reporter = BenchmarkFileReporter("MaxThroughputSpec", system)
    for (s ← scenarios) {
      s"be great for ${s.testName}, burstSize = ${s.burstSize}, payloadSize = ${s.payloadSize}" in test(s, reporter)
    }
  }
}
