package csw.services.integtration.apps

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType, ResolvedAkkaLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object AssemblyApp {
  private val actorRuntime = new ActorRuntime("crdt", "eth1")

  val assemblyActorRef = actorRuntime.actorSystem.actorOf(Props[AssemblyApp], "assembly")
  val componentId = ComponentId("assembly", ComponentType.Assembly)
  val connection = AkkaConnection(componentId)

  val actorPath = ActorPath.fromString(Serialization.serializedActorPath(assemblyActorRef))
  val registration = new ResolvedAkkaLocation(connection, assemblyActorRef)
  val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

  def main(args: Array[String]): Unit = {

  }

}

class AssemblyApp extends Actor {
  override def receive: Receive = {
    case "Unregister" => {
      AssemblyApp.registrationResult.unregister()
    }
  }
}
