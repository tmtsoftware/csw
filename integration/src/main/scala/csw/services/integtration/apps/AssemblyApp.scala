package csw.services.integtration.apps

import akka.actor.{Actor, ActorPath, ActorRef, Props}
import akka.serialization.Serialization
import akka.typed.scaladsl.adapter._
import csw.param.models.location.Connection.AkkaConnection
import csw.param.models.location.{ComponentId, ComponentType}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationServiceFactory}

object AssemblyApp {
  private val cswCluster = CswCluster.withSettings(ClusterSettings().withInterface("eth1"))

  val assemblyActorRef: ActorRef = ActorSystemFactory.remote().actorOf(Props[AssemblyApp], "assembly")
  val componentId                = ComponentId("assembly", ComponentType.Assembly)
  val connection                 = AkkaConnection(componentId)

  val actorPath: ActorPath = ActorPath.fromString(Serialization.serializedActorPath(assemblyActorRef))
  val registration         = AkkaRegistration(connection, assemblyActorRef)
  val registrationResult: RegistrationResult =
    LocationServiceFactory.withCluster(cswCluster).register(registration).await

  def main(args: Array[String]): Unit = {}

}

class AssemblyApp extends Actor {
  override def receive: Receive = {
    case "Unregister" => AssemblyApp.registrationResult.unregister()
  }
}
