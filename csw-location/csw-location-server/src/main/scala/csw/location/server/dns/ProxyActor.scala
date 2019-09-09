package csw.location.server.dns

import akka.actor
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ActorSystem, Props}
import com.github.mkroli.dns4s.Message
import csw.location.server.dns.LocationDnsActor.{DnsActorMessage, StandardMessage}

class ProxyActor(targetActor: ActorRef[DnsActorMessage]) extends Actor {
  override def receive: Receive = {
    case message: Message => targetActor ! StandardMessage(message, sender().toTyped[Message])
  }
}

object ProxyActor {
  def start(targetActor: ActorRef[DnsActorMessage])(implicit actorSystem: ActorSystem): actor.ActorRef = {
    actorSystem.actorOf(Props(new ProxyActor(targetActor)))
  }
}
