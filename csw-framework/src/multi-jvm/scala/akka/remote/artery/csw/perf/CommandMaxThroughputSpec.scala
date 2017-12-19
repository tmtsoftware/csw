package akka.remote.artery.csw.perf

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.NANOSECONDS

import _root_.csw.common.FrameworkAssertions
//import _root_.csw.services.logging.scaladsl.LoggingSystemFactory
//import _root_.csw.services.logging.appenders.StdOutAppender
import _root_.csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import _root_.csw.messages.SupervisorContainerCommonMessages.Shutdown
import _root_.csw.messages.ccs.commands.ComponentRef
import _root_.csw.messages.framework.SupervisorLifecycleState
import _root_.csw.messages.location.Connection.AkkaConnection
import _root_.csw.messages.location.{ComponentId, ComponentType}
import _root_.csw.services.location.commons.ClusterAwareSettings
import _root_.csw.services.location.helpers.{LSNodeSpecForPerf, OneMemberAndSeedForPerf}
import akka.actor._
import akka.remote.artery._
import akka.remote.artery.compress.CompressionProtocol.Events.ReceivedActorRefCompressionTable
import akka.remote.artery.csw.component._
import akka.remote.{RARP, RemoteActorRefProvider}
import akka.testkit._
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.{scaladsl, TestKitSettings}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object CommandMaxThroughputSpec {
  case object Run
  final case class EndResult(totalReceived: Long) extends JavaSerializable

  def senderProps(
      componentRef: ComponentRef,
      testSettings: TestSettings,
      plotRef: ActorRef,
      printTaskRunnerMetrics: Boolean,
      reporter: BenchmarkFileReporter
  ): Props =
    Props(new Sender(componentRef, testSettings, plotRef, printTaskRunnerMetrics, reporter))

  class Sender(
      componentRef: ComponentRef,
      testSettings: TestSettings,
      plotRef: ActorRef,
      printTaskRunnerMetrics: Boolean,
      reporter: BenchmarkFileReporter
  ) extends Actor {

    import testSettings._
    val payload: Array[Byte] = ("0" * testSettings.payloadSize).getBytes("utf-8")
    var startTime            = 0L
    var remaining: Long      = totalMessages
    var maxRoundTripMillis   = 0L
    val taskRunnerMetrics    = new TaskRunnerMetrics(context.system)

    context.system.eventStream.subscribe(self, classOf[ReceivedActorRefCompressionTable])

    var flowControlId      = 0
    var pendingFlowControl = Map.empty[Int, Int]

    def receive: Receive = {
      case Run ⇒ runWarmup()
    }

    def runWarmup(): Unit = {
      sendBatch(warmup = true)         // first some warmup
      componentRef.value ! Start(self) // then Start, which will echo back here
      context.become(warmup)
    }

    def warmup: Receive = {
      case Start ⇒
        println(
          s"${self.path.name}: Starting benchmark of $totalMessages messages with burst size " + s"$burstSize and payload size $payloadSize"
        )
        startTime = System.nanoTime
        remaining = totalMessages
        (0 until sent.length).foreach(i ⇒ sent(i) = 0)
        // have a few batches in flight to make sure there are always messages to send
        (1 to 3).foreach { _ ⇒
          val t0 = System.nanoTime()
          sendBatch(warmup = false)
          sendFlowControl(t0)
        }
        context.become(active)
      case _: Warmup ⇒
    }

    def active: Receive = {
      case c @ FlowControl(id, t0) ⇒
        val targetCount = pendingFlowControl(id)
        if (targetCount - 1 == 0) {
          pendingFlowControl -= id
          val now      = System.nanoTime()
          val duration = NANOSECONDS.toMillis(now - t0)
          maxRoundTripMillis = math.max(maxRoundTripMillis, duration)

          sendBatch(warmup = false)
          sendFlowControl(now)
        } else {
          // waiting for FlowControl from more targets
          pendingFlowControl = pendingFlowControl.updated(id, targetCount - 1)
        }
    }

    val waitingForEndResult: Receive = {
      case EndResult(totalReceived) ⇒
        val took       = NANOSECONDS.toMillis(System.nanoTime - startTime)
        val throughput = totalReceived * 1000.0 / took

        reporter.reportResults(
          s"=== ${reporter.testName} ${self.path.name}: " +
          f"throughput ${throughput * testSettings.senderReceiverPairs}%,.0f msg/s, " +
          f"${throughput * payloadSize * testSettings.senderReceiverPairs}%,.0f bytes/s (payload), " +
          f"${throughput * totalSize(context.system) * testSettings.senderReceiverPairs}%,.0f bytes/s (total" +
          (if (RARP(context.system).provider.remoteSettings.Artery.Advanced.Compression.Enabled) ",compression" else "") + "), " +
          (if (testSettings.senderReceiverPairs == 1) s"dropped ${totalMessages - totalReceived}, " else "") +
          s"max round-trip $maxRoundTripMillis ms, " +
          s"burst size $burstSize, " +
          s"payload size $payloadSize, " +
          s"total size ${totalSize(context.system)}, " +
          s"$took ms to deliver $totalReceived messages."
        )

        if (printTaskRunnerMetrics)
          taskRunnerMetrics.printHistograms()

        plotRef ! PlotResult().add(testName, throughput * payloadSize * testSettings.senderReceiverPairs / 1024 / 1024)
        context.stop(self)

      case c: ReceivedActorRefCompressionTable ⇒
    }

    val sent = new Array[Long](1)
    def sendBatch(warmup: Boolean): Unit = {
      val batchSize = math.min(remaining, burstSize)
      var i         = 0
      while (i < batchSize) {
        val msg0 =
          TestMessage(
            id = totalMessages - remaining + i,
            name = "abc",
            status = i % 2 == 0,
            description = "ABC",
            payload = payload,
            items = Vector(TestMessage.Item(1, "A"), TestMessage.Item(2, "B"))
          )

        val msg1: TopLevelActorDomainMessage = if (warmup) Warmup(msg0) else msg0

        componentRef.value ! msg1
        sent(i % 1) += 1
        i += 1
      }
      remaining -= batchSize
    }

    def sendFlowControl(t0: Long): Unit = {
      if (remaining <= 0) {
        context.become(waitingForEndResult)
        componentRef.value ! End
      } else {
        flowControlId += 1
        pendingFlowControl = pendingFlowControl.updated(flowControlId, 1)
        val flowControlMsg = FlowControl(flowControlId, t0)
        self ! flowControlMsg
      }
    }
  }
}

class CommandMaxThroughputSpecMultiJvmNode1 extends CommandMaxThroughputSpec
class CommandMaxThroughputSpecMultiJvmNode2 extends CommandMaxThroughputSpec

abstract class CommandMaxThroughputSpec extends LSNodeSpecForPerf(config = new OneMemberAndSeedForPerf) with PerfFlamesSupport {
//abstract class MaxThroughputSpec extends RemotingMultiNodeSpec(MaxThroughputSpec) with PerfFlamesSupport {

  import CommandMaxThroughputSpec._
  import config._

//  val loggingSystem = LoggingSystemFactory.start("", "", "", system)
//  loggingSystem.setAppenders(List(StdOutAppender))

  val totalMessagesFactor = system.settings.config.getDouble("akka.test.MaxThroughputSpec.totalMessagesFactor")
  val realMessage         = system.settings.config.getBoolean("akka.test.MaxThroughputSpec.real-message")
  val actorSelection      = system.settings.config.getBoolean("akka.test.MaxThroughputSpec.actor-selection")

  var plot = PlotResult()

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  override def initialParticipants = roles.size

  def remoteSettings = system.asInstanceOf[ExtendedActorSystem].provider.asInstanceOf[RemoteActorRefProvider].remoteSettings

  lazy val reporterExecutor = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(seed) {
      println(plot.csv(system.name))
    }
    super.afterAll()
  }

  val scenarios = List(
    TestSettings(testName = "warmup",
                 totalMessages = adjustedTotalMessages(20000),
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
                 realMessage)
//    TestSettings(
//      testName = "5-to-5",
//      totalMessages = adjustedTotalMessages(20000),
//      burstSize = 200, // don't exceed the send queue capacity 200*5*3=3000
//      payloadSize = 100,
//      senderReceiverPairs = 5,
//      realMessage
//    )
  )

  def test(testSettings: TestSettings, resultReporter: BenchmarkFileReporter): Unit = {
    import testSettings._
    val receiverName             = testName + "-rcv"
    implicit val typed           = system.toTyped
    implicit val testKitSettings = TestKitSettings(typed)

    runPerfFlames(seed, member)(delay = 5.seconds, time = 15.seconds)

    runOn(member) {
      val rep = reporter(testName)

      val probe   = scaladsl.TestProbe[SupervisorLifecycleState]
      val system2 = ClusterAwareSettings.joinLocal(3552).system
      // spawn single hcd running in Standalone mode in jvm-3
      val wiring         = FrameworkWiring.make(system2)
      val standaloneConf = ConfigFactory.load("perf/standalone.conf")
      val actorRef       = Await.result(Standalone.spawn(standaloneConf, wiring), 5.seconds)
      FrameworkAssertions.assertThatSupervisorIsRunning(actorRef, probe, 5.seconds)

      actorRef ! InitComponentForPerfTest(rep, payloadSize, printTaskRunnerMetrics = true)
      enterBarrier(receiverName + "-started")
      enterBarrier(testName + "-done")
      actorRef ! Shutdown
      Await.ready(system2.whenTerminated, 15.seconds)
      rep.halt()
    }

    runOn(seed) {
      enterBarrier(receiverName + "-started")
      val hcdLocF = locationService.resolve(AkkaConnection(ComponentId("Eton", ComponentType.HCD)), 5.seconds)

      val hcdComponent: ComponentRef = Await.result(hcdLocF, 5.seconds).map(_.component).get

      val senders = for (n ← 1 to senderReceiverPairs) yield {
        val plotProbe = TestProbe()
        val snd = system.actorOf(
          senderProps(hcdComponent, testSettings, plotProbe.ref, printTaskRunnerMetrics = n == 1, resultReporter),
          testName + "-snd" + n
        )
        val terminationProbe = TestProbe()
        terminationProbe.watch(snd)
        snd ! Run
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

  val reporter = BenchmarkFileReporter("MaxThroughputSpec", system)
  for (s ← scenarios) {
    test(s"Max throughput of Artery be great for ${s.testName}, burstSize = ${s.burstSize}, payloadSize = ${s.payloadSize}") {
      test(s, reporter)
    }
  }
}
