package csw.services.integtration.apps

import akka.actor.{Actor, Props}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object AssemblyApp extends App {
  private val actorRuntime = new ActorRuntime("assembly", "eth1")

  val assemblyActorRef = actorRuntime.actorSystem.actorOf(Props[AssemblyApp], "assembly")
  val componentId = ComponentId("assembly", ComponentType.Assembly)
  val connection = AkkaConnection(componentId)

  val registration = AkkaRegistration(connection, assemblyActorRef, "tmt.assembly")
  val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

}

class AssemblyApp extends Actor{
  override def receive: Receive = {
 	case "Unregister" => {
      	AssemblyApp.registrationResult.unregister()
    }
}
}
