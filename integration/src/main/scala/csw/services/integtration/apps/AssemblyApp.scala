package csw.services.integtration.apps

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType, ResolvedAkkaLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object AssemblyApp extends App {
  private val actorRuntime = new ActorRuntime("assembly", "eth1", 2552)

  val assemblyActorRef = actorRuntime.actorSystem.actorOf(Props[AssemblyApp], "assembly")
  val componentId = ComponentId("assembly", ComponentType.Assembly)
  val connection = AkkaConnection(componentId)

  val actorPath = ActorPath.fromString(Serialization.serializedActorPath(assemblyActorRef))
  val uri = new URI(actorPath.toString)
  val registration = ResolvedAkkaLocation(connection, uri, "tmt.assembly", Some(assemblyActorRef))
  lazy val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

}

class AssemblyApp extends Actor {
  override def receive: Receive = {
    case "Unregister" => {
      AssemblyApp.registrationResult.unregister()
    }
  }
}
