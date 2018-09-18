package csw.integtration.apps

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{typed, Actor, ActorPath, ActorRef, ActorSystem, Props}
import akka.serialization.Serialization
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.commons.ClusterSettings
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaRegistration, ComponentId, ComponentType, RegistrationResult}
import csw.location.commons.{ActorSystemFactory, CswCluster}
import csw.location.scaladsl.LocationServiceFactory
import csw.logging.messages.LogControlMessages
import csw.params.core.models.Prefix

object AssemblyApp {
  private val cswCluster = CswCluster.withSettings(ClusterSettings().withInterface("eth1"))

  private val actorSystem: ActorSystem                     = ActorSystemFactory.remote()
  val assemblyActorRef: ActorRef                           = actorSystem.actorOf(Props[AssemblyApp], "assembly")
  val logAdminActorRef: typed.ActorRef[LogControlMessages] = actorSystem.spawn(Behavior.empty, "my-actor-1-admin")
  val componentId                                          = ComponentId("assembly", ComponentType.Assembly)
  val connection                                           = AkkaConnection(componentId)

  val actorPath: ActorPath = ActorPath.fromString(Serialization.serializedActorPath(assemblyActorRef))
  val registration         = AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), assemblyActorRef, logAdminActorRef)
  val registrationResult: RegistrationResult =
    LocationServiceFactory.withCluster(cswCluster).register(registration).await

  def main(args: Array[String]): Unit = {}

}

class AssemblyApp extends Actor {
  override def receive: Receive = {
    case "Unregister" => AssemblyApp.registrationResult.unregister()
  }
}
