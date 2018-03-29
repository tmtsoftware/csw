package csw.services.event.perf

import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import csw.services.event.RedisFactory
import csw.services.event.internal.commons.Wiring
import csw.services.event.perf.EventUtils._
import csw.services.event.perf.EventThroughputSpec.Target
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PublishingActor(
    target: Target,
    targets: Array[Target],
    testSettings: TestSettings,
    plotRef: ActorRef,
    printTaskRunnerMetrics: Boolean,
    reporter: BenchmarkFileReporter
) extends Actor
    with MockitoSugar {

  val numTargets = targets.size

  import testSettings._
  val payload            = ("0" * testSettings.payloadSize).getBytes("utf-8")
  var startTime          = 0L
  var remaining          = totalMessages
  var maxRoundTripMillis = 0L
  val taskRunnerMetrics  = new TaskRunnerMetrics(context.system)

  var flowControlId      = 0
  var pendingFlowControl = Map.empty[Int, Int]

  import Messages._

  private implicit val actorSystem: ActorSystem = context.system
  private implicit val mat: ActorMaterializer   = ActorMaterializer()

  private val redisHost    = "localhost"
  private val redisPort    = 6379
  private val redisClient  = RedisClient.create()
  private val wiring       = new Wiring(actorSystem)
  private val redisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
  private val publisher    = redisFactory.publisher(redisHost, redisPort)

  def receive: Receive = {
    case Init        ⇒ targets.foreach(_.tell(Init(target.ref), self))
    case Initialized ⇒ runWarmup()
  }

  def runWarmup(): Unit = {
    sendBatch(warmup = true) // first some warmup
    publisher.publish(makeEvent(startEventName))
    context.become(warmup)
  }

  def warmup: Receive = {
    case Start ⇒
      println("======================================")
      println(
        s"${self.path.name}: Starting benchmark of $totalMessages messages with burst size " +
        s"$burstSize and payload size $payloadSize"
      )

      startTime = System.nanoTime
      remaining = totalMessages
      sent.indices.foreach(i ⇒ sent(i) = 0)
      // have a few batches in flight to make sure there are always messages to send
      (1 to 3).foreach { _ ⇒
        val t0 = System.nanoTime()
        sendBatch(warmup = false)
        sendFlowControl(t0)
      }

      context.become(active)
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
  val sent = new Array[Long](targets.length)

  def waitingForEndResult: Receive = {
    case EndResult(totalReceived) ⇒
      val took       = NANOSECONDS.toMillis(System.nanoTime - startTime)
      val throughput = totalReceived * 1000.0 / took

      println("======================================")
      reporter.reportResults(
        s"=== ${reporter.testName} ${self.path.name}: " +
        f"throughput ${throughput * testSettings.senderReceiverPairs}%,.0f msg/s, " +
        f"${throughput * payloadSize * testSettings.senderReceiverPairs}%,.0f bytes/s (payload), " +
        f"${throughput * totalSize(context.system) * testSettings.senderReceiverPairs}%,.0f bytes/s (total" +
        (if (testSettings.senderReceiverPairs == 1) s"dropped ${totalMessages - totalReceived}, " else "") +
        s"max round-trip $maxRoundTripMillis ms, " +
        s"burst size $burstSize, " +
        s"payload size $payloadSize, " +
        s"total size ${totalSize(context.system)}, " +
        s"$took ms to deliver $totalReceived messages, " +
        s"Total events sent by publisher to subscribers: ${sent.mkString(",")}"
      )

      if (printTaskRunnerMetrics)
        taskRunnerMetrics.printHistograms()

      plotRef ! PlotResult().add(testName, throughput * payloadSize * testSettings.senderReceiverPairs / 1024 / 1024)
      context.stop(self)

  }

  def sendBatch(warmup: Boolean): Unit = {
    val batchSize = math.min(remaining, burstSize)

    Await.result(
      Source(0 until batchSize.toInt)
        .map { counter ⇒
          sent(counter % numTargets) += 1
          if (warmup) makeEvent(warmupEventName, payload = payload)
          else makeEvent(eventName, totalMessages - remaining + counter, payload)
        }
        .mapAsync(1)(publisher.publish)
        .runWith(Sink.ignore),
      10.seconds
    )

    remaining -= batchSize
  }

  def sendFlowControl(t0: Long): Unit = {
    if (remaining <= 0) {
      context.become(waitingForEndResult)
      publisher.publish(makeEvent(endEventName))
    } else {
      flowControlId += 1
      pendingFlowControl = pendingFlowControl.updated(flowControlId, targets.size)
      val flowControlEvent = makeFlowCtlEvent(flowControlId, t0)
      publisher.publish(flowControlEvent)
    }
  }
}

object PublishingActor {

  def props(
      mainTarget: Target,
      targets: Array[Target],
      testSettings: TestSettings,
      plotRef: ActorRef,
      printTaskRunnerMetrics: Boolean,
      reporter: BenchmarkFileReporter
  ): Props =
    Props(new PublishingActor(mainTarget, targets, testSettings, plotRef, printTaskRunnerMetrics, reporter))

}
