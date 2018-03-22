package csw.services.event.internal

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix

class EventStoreActor extends Actor {

  var latestEvent: Event = SystemEvent(Prefix("invalid"), EventName("invalid"))

  override def receive: Receive = {
    case event: Event ⇒ {
      latestEvent = event
    }
    case "getLatest" ⇒ {
      sender() ! latestEvent
    }
    case "streamCompleted" ⇒ //Do Something
  }
}

object EventStoreActor {
  def apply(): ActorRef = ActorSystem("test").actorOf(Props(new EventStoreActor))
}

class DummyActor extends Actor {

  override def receive: Receive = {
    case _ ⇒
  }
}

object DummyActor {
  def apply(): ActorRef = ActorSystem("test").actorOf(Props(new DummyActor))
}
