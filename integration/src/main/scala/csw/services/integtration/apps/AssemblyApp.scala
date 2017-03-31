package csw.services.integtration.apps

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.internal.Settings
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{CswCluster, LocationServiceFactory}

object AssemblyApp {
  private val cswCluster = CswCluster.withSettings(Settings().withInterface("eth1"))

  val assemblyActorRef = cswCluster.actorSystem.actorOf(Props[AssemblyApp], "assembly")
  val componentId = ComponentId("assembly", ComponentType.Assembly)
  val connection = AkkaConnection(componentId)

  val actorPath = ActorPath.fromString(Serialization.serializedActorPath(assemblyActorRef))
  val registration = AkkaRegistration(connection, assemblyActorRef)
  val registrationResult = LocationServiceFactory.make(cswCluster).register(registration).await

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
