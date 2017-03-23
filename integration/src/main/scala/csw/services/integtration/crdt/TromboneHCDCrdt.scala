package csw.services.integtration.crdt

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.pattern.pipe
import akka.serialization.Serialization
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.internal.LocationServiceCrdtImpl
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType, ResolvedAkkaLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TromboneHCDCrdt {
  private val actorRuntime = new ActorRuntime("crdt", 2552)

  val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCDCrdt], "trombone-hcd")
  val componentId = ComponentId("trombonehcd", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  val actorPath = ActorPath.fromString(Serialization.serializedActorPath(tromboneHcdActorRef))
  val uri = new URI(actorPath.toString)
  val location = ResolvedAkkaLocation(connection, uri, "nfiraos.ncc.tromboneHCD", Some(tromboneHcdActorRef))
  val serviceCrdtImpl = LocationServiceFactory.make(actorRuntime)
  val registrationResult = serviceCrdtImpl.register(location).await
  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {

  }
}

class TromboneHCDCrdt extends Actor {
  import context.dispatcher
  override def receive: Receive = {
    case "Unregister" => {
      println("Unregistered the connection.")
      TromboneHCDCrdt.registrationResult.unregister() pipeTo sender()
    }
    case "Test" => println("Received test message.")
  }
}
