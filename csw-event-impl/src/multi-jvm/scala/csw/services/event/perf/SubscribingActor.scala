package csw.services.event.perf

import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import csw.messages.events._
import csw.services.event.internal.commons.Wiring
import csw.services.event.perf.EventUtils._
import csw.services.event.scaladsl.RedisFactory
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.HdrHistogram.Histogram
import org.scalatest.mockito.MockitoSugar

class SubscribingActor(reporter: RateReporter, payloadSize: Int, numSenders: Int, id: Int) extends Actor with MockitoSugar {

  private var eventsReceived                = 0L
  private var endMessagesMissing            = numSenders
  private var correspondingSender: ActorRef = null // the Actor which send the Start message will also receive the report
  private var publishers: List[ActorRef]    = Nil

  val histogram = new Histogram(SECONDS.toNanos(10), 3)

  import Messages._

  private implicit val actorSystem: ActorSystem = context.system

  private val redisHost    = "localhost"
  private val redisPort    = 6379
  private val redisClient  = RedisClient.create()
  private val wiring       = new Wiring(actorSystem)
  private val redisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
  private val subscriber   = redisFactory.subscriber(redisHost, redisPort)
  var startTime            = 0L
  var reportedArrayOOB     = false

  private val keys: Set[EventKey] = eventKeys + EventKey(s"$testEventKey.$id")
  startSubscription(keys)

  private def startSubscription(eventKeys: Set[EventKey]) = subscriber.subscribeCallback(eventKeys, onEvent)

  private def onEvent(event: Event): Unit = {
    event match {
      case SystemEvent(_, _, `warmupEvent`, _, _) ⇒
      case SystemEvent(_, _, `startEvent`, _, _) ⇒ {
        startTime = System.nanoTime()
        correspondingSender ! Start
      }
      case SystemEvent(_, _, `endEvent`, _, _) if endMessagesMissing > 1 ⇒
        endMessagesMissing -= 1 // wait for End message from all senders

      case SystemEvent(_, _, `endEvent`, _, _) ⇒
        correspondingSender ! EndResult(eventsReceived, histogram, System.nanoTime() - startTime)
        context.stop(self)

      case event @ SystemEvent(_, _, `flowControlEvent`, _, _) ⇒
        val flowCtlId      = event.eventId.id.toInt
        val burstStartTime = event.get(flowCtlKey).get.value(0)
        val publisher      = event.get(publisherKey).get.value(0)

        val sender = publishers.find(_.path.name.equalsIgnoreCase(publisher)).get
        sender ! FlowControl(flowCtlId, burstStartTime)

      case Event.invalidEvent ⇒
      case event: SystemEvent ⇒ report(event.get(timeNanosKey).get.head)
    }
  }

  def receive: PartialFunction[Any, Unit] = {
    case Init(corresponding) ⇒
      if (corresponding == self || numSenders == 1) correspondingSender = sender()

      publishers = sender() :: publishers
      sender() ! Initialized
  }

  def report(sendTime: Long): Unit = {
    if (eventsReceived == 0)
      startTime = System.nanoTime()

    reporter.onMessage(1, payloadSize)
    eventsReceived += 1

    val d = System.nanoTime() - sendTime
    try {
      histogram.recordValue(d)
    } catch {
      case e: ArrayIndexOutOfBoundsException ⇒
        // Report it only once instead of flooding the console
        if (!reportedArrayOOB) {
          e.printStackTrace()
          reportedArrayOOB = true
        }
    }
  }
}

object SubscribingActor {

  def props(reporter: RateReporter, payloadSize: Int, numSenders: Int, id: Int): Props =
    Props(new SubscribingActor(reporter, payloadSize, numSenders, id)).withDispatcher("akka.remote.default-remote-dispatcher")
}
