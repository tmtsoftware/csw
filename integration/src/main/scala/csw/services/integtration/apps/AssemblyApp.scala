package csw.services.integtration.apps

import akka.actor.{Actor, ActorPath, ActorRef, ActorSystem, Props}
import akka.serialization.Serialization
import akka.typed
import akka.typed.Behavior
import akka.typed.scaladsl.adapter._
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.{ActorSystemFactory, ClusterSettings, CswCluster}
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.internal.LogControlMessages

object AssemblyApp {
  private val cswCluster = CswCluster.withSettings(ClusterSettings().withInterface("eth1"))

  private val actorSystem: ActorSystem                     = ActorSystemFactory.remote()
  val assemblyActorRef: ActorRef                           = actorSystem.actorOf(Props[AssemblyApp], "assembly")
  val logAdminActorRef: typed.ActorRef[LogControlMessages] = actorSystem.spawn(Behavior.empty, "my-actor-1-admin")
  val componentId                                          = ComponentId("assembly", ComponentType.Assembly)
  val connection                                           = AkkaConnection(componentId)

  val actorPath: ActorPath = ActorPath.fromString(Serialization.serializedActorPath(assemblyActorRef))
  val registration         = AkkaRegistration(connection, Some("nfiraos.ncc.trombone"), assemblyActorRef, logAdminActorRef)
  val registrationResult: RegistrationResult =
    LocationServiceFactory.withCluster(cswCluster).register(registration).await

  def main(args: Array[String]): Unit = {}

}

class AssemblyApp extends Actor {
  override def receive: Receive = {
    case "Unregister" => AssemblyApp.registrationResult.unregister()
  }
}
