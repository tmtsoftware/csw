package csw.services.integtration.apps

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.pattern.pipe
import akka.serialization.Serialization
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType, AkkaLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TromboneHCD {
  private val actorRuntime = new ActorRuntime("crdt")

  val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId = ComponentId("trombonehcd", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  val actorPath = ActorPath.fromString(Serialization.serializedActorPath(tromboneHcdActorRef))
  val registration = new AkkaLocation(connection, tromboneHcdActorRef)
  val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {

  }
}

class TromboneHCD extends Actor {
  import context.dispatcher
  override def receive: Receive = {
    case "Unregister" => {
      println("Unregistered the connection.")
      TromboneHCD.registrationResult.unregister() pipeTo sender()
    }
    case "Test" => println("Received test message.")
  }
}
