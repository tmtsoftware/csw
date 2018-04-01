package csw.services.event.perf

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import csw.messages.events.EventName
import csw.services.event.RedisFactory
import csw.services.event.internal.commons.Wiring
import csw.services.event.perf.EventThroughputSpec.Target
import csw.services.event.perf.EventUtils._
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class PublishingActor(
    target: Target,
    targets: Array[Target],
    testSettings: TestSettings,
    plotRef: ActorRef,
    printTaskRunnerMetrics: Boolean,
    reporter: BenchmarkFileReporter
) extends Actor
    with MockitoSugar {

  val numTargets: Int = targets.length

  import testSettings._
  val payload: Array[Byte] = ("0" * testSettings.payloadSize).getBytes("utf-8")
  var startTime            = 0L
  var remaining: Long      = totalMessages
  var maxRoundTripMillis   = 0L
  val taskRunnerMetrics    = new TaskRunnerMetrics(context.system)

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

  val throttlingElements: Int = actorSystem.settings.config.getInt("csw.test.EventThroughputSpec.throttling.elements")
  val throttlingDuration: FiniteDuration = {
    val d = actorSystem.settings.config.getDuration("csw.test.EventThroughputSpec.throttling.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }

  def receive: Receive = {
    case Init        ⇒ targets.foreach(_.tell(Init(target.ref), self))
    case Initialized ⇒ runWarmup()
  }

  def runWarmup(): Unit = {
    sendBatch(warmup = true) // first some warmup
    publisher.publish(event(startEvent))
    context.become(warmup)
  }

  def warmup: Receive = {
    case Start ⇒
      println("======================================")
      println(
        s"${self.path.name}: Starting benchmark of $totalMessages messages with " +
        s"${if (batching) s"burst size $burstSize"
        else s"throttling of ${throttlingElements}msgs/${throttlingDuration.toSeconds}s"} " +
        s"and payload size $payloadSize"
      )

      startTime = System.nanoTime
      remaining = totalMessages
      sent.indices.foreach(i ⇒ sent(i) = 0)

      context.become(active)
      val t0 = System.nanoTime()
      sendBatch(warmup = false)
      sendFlowControl(t0)
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
        s"dropped ${totalMessages - totalReceived}, " +
        s"max round-trip $maxRoundTripMillis ms, " +
        s"burst size $burstSize, " +
        s"payload size $payloadSize, " +
        s"total size ${totalSize(context.system)}, " +
        s"$took ms to deliver $totalReceived messages"
      )

      if (printTaskRunnerMetrics)
        taskRunnerMetrics.printHistograms()

      plotRef ! PlotResult().add(testName, throughput * payloadSize * testSettings.senderReceiverPairs / 1024 / 1024)
      context.stop(self)

  }

  def sendBatch(warmup: Boolean): Unit = {

    def makeEvent(counter: Int, id: Int) = {
      if (warmup) event(warmupEvent, payload = payload)
      else event(EventName(s"$testEvent.${id + 1}"), totalMessages - remaining + counter, payload)
    }

    val batchSize =
      if (warmup) 1000
      else if (batching) math.min(remaining, burstSize)
      else totalMessages

    if (batching) {
      var i = 0
      while (i < batchSize) {
        val id = i % numTargets
        sent(id) += 1

        Await.result(publisher.publish(makeEvent(i, id)), 5.seconds)
        i += 1
      }
      remaining -= batchSize
    } else {

      Await.result(
        Source(0 until batchSize.toInt)
          .throttle(throttlingElements, throttlingDuration, throttlingElements, ThrottleMode.Shaping)
          .map { counter ⇒
            val id = counter % numTargets
            sent(id) += 1

            makeEvent(counter, id)
          }
          .map(publisher.publish)
          .runWith(Sink.ignore),
        5.minutes
      )

      remaining -= totalMessages
    }
  }

  def sendFlowControl(t0: Long): Unit = {
    if (remaining <= 0) {
      context.become(waitingForEndResult)
      publisher.publish(event(endEvent))
    } else {
      flowControlId += 1
      pendingFlowControl = pendingFlowControl.updated(flowControlId, targets.length)
      Await.result(publisher.publish(flowCtlEvent(flowControlId, t0, self.path.name)), 5.seconds)
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
