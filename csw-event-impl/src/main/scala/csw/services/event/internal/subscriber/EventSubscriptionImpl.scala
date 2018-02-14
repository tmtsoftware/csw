package csw.services.event.internal.subscriber

import akka.Done
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.{EventServiceDriver, EventSubscription}

import scala.concurrent.{ExecutionContext, Future}

class EventSubscriptionImpl(
    eventServiceDriver: EventServiceDriver,
    callback: Event ⇒ Unit,
    eventKeys: Seq[EventKey]
)(implicit val mat: Materializer, ec: ExecutionContext)
    extends EventSubscription {

  private lazy val (signal, switch): (Future[Done], UniqueKillSwitch) = eventServiceDriver
    .subscribe(eventKeys.map(_.toString))
    .viaMat(KillSwitches.single)(Keep.both)
    .to(Sink.foreach(ch ⇒ callback(Event.fromPb(ch.getMessage))))
    .run

  def subscribe(): Future[Done] = signal.map(x => Done)

  override def unsubscribe(): Future[Done] = {
    eventServiceDriver
      .unsubscribe(eventKeys.map(_.toString))
      .transform(x ⇒ { switch.shutdown(); x }, ex ⇒ { switch.abort(ex); ex })
  }
}
