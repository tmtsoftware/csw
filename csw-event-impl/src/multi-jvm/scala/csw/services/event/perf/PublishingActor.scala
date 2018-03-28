package csw.services.event.perf

import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor._
import csw.services.event.RedisFactory
import csw.services.event.internal.commons.Wiring
import csw.services.event.perf.Helpers.{makeEvent, warmupEvent}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

sealed trait Target {
  def tell(msg: Any, sender: ActorRef): Unit
  def ref: ActorRef
}

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

  private val redisHost    = "localhost"
  private val redisPort    = 6379
  private val redisClient  = RedisClient.create()
  private val wiring       = new Wiring(actorSystem)
  private val redisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
  private val publisher    = redisFactory.publisher(redisHost, redisPort)

  def receive: Receive = {
    case Run ⇒ runWarmup()
  }

  def runWarmup(): Unit = {
    sendBatch(warmup = true)                         // first some warmup
    targets.foreach(_.tell(Start(target.ref), self)) // then Start, which will echo back here
    context.become(warmup)
  }

  def warmup: Receive = {
    case Start ⇒
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

  }

  val sent = new Array[Long](targets.length)
  def sendBatch(warmup: Boolean): Unit = {
    val batchSize = math.min(remaining, burstSize)
    var i         = 0
    while (i < batchSize) {
      // Fixme: create event of size = payload size
      val msg1 = if (warmup) warmupEvent else makeEvent(totalMessages - remaining + i)

      publisher.publish(msg1)
      sent(i % numTargets) += 1
      i += 1
    }
    remaining -= batchSize
  }

  def sendFlowControl(t0: Long): Unit = {
    if (remaining <= 0) {
      context.become(waitingForEndResult)
      targets.foreach(_.tell(End, self))
    } else {
      flowControlId += 1
      pendingFlowControl = pendingFlowControl.updated(flowControlId, targets.size)
      val flowControlMsg = FlowControl(flowControlId, t0)
      targets.foreach(_.tell(flowControlMsg, self))
    }
  }
}
