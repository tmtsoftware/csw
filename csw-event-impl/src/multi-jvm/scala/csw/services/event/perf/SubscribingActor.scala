package csw.services.event.perf

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import csw.messages.events._
import csw.services.event.RedisFactory
import csw.services.event.internal.commons.Wiring
import csw.services.event.perf.EventUtils._
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

class SubscribingActor(reporter: RateReporter, payloadSize: Int, printTaskRunnerMetrics: Boolean, numSenders: Int, id: Int)
    extends Actor
    with MockitoSugar {

  private var eventsReceived                = 0L
  private val taskRunnerMetrics             = new TaskRunnerMetrics(context.system)
  private var endMessagesMissing            = numSenders
  private var correspondingSender: ActorRef = null // the Actor which send the Start message will also receive the report

  import Messages._

  private implicit val actorSystem: ActorSystem = context.system

  private val redisHost    = "localhost"
  private val redisPort    = 6379
  private val redisClient  = RedisClient.create()
  private val wiring       = new Wiring(actorSystem)
  private val redisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
  private val subscriber   = redisFactory.subscriber(redisHost, redisPort)

  private val keys: Set[EventKey] = eventKeys + EventKey(s"$eventKey.$id")
  startSubscription(keys)

  private def startSubscription(eventKeys: Set[EventKey]) = subscriber.subscribeCallback(eventKeys, onEvent)

  private def onEvent(event: Event): Unit = {
    event match {
      case SystemEvent(_, _, `warmupEventName`, _, _) ⇒
      case SystemEvent(_, _, `startEventName`, _, _)  ⇒ correspondingSender ! Start
      case SystemEvent(_, _, `endEventName`, _, _) if endMessagesMissing > 1 ⇒
        endMessagesMissing -= 1 // wait for End message from all senders

      case SystemEvent(_, _, `endEventName`, _, _) ⇒
        if (printTaskRunnerMetrics)
          taskRunnerMetrics.printHistograms()
        correspondingSender ! EndResult(eventsReceived)
        context.stop(self)

      case SystemEvent(id, _, `flowControlEventName`, _, paramSet) ⇒
        val flowCtlId      = id.id.toInt
        val burstStartTime = paramSet.head.value(0).asInstanceOf[Long]
        correspondingSender ! FlowControl(flowCtlId, burstStartTime)

      case Event.invalidEvent ⇒
      case _: Event           ⇒ report()
    }
  }

  def receive: PartialFunction[Any, Unit] = {
    case Init(corresponding) ⇒
      if (corresponding == self) correspondingSender = sender()
      sender() ! Initialized
  }

  def report(): Unit = {
    reporter.onMessage(1, payloadSize)
    eventsReceived += 1
  }
}

object SubscribingActor {

  def props(reporter: RateReporter, payloadSize: Int, printTaskRunnerMetrics: Boolean, numSenders: Int, id: Int): Props =
    Props(new SubscribingActor(reporter, payloadSize, printTaskRunnerMetrics, numSenders, id))
      .withDispatcher("akka.remote.default-remote-dispatcher")
}
