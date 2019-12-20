package csw.integtration.apps

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.RegistrationResult
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.impl.commons.ClusterAwareSettings
import csw.location.impl.internal.ServerWiring
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.prefix.models.{Prefix, Subsystem}

object AssemblyApp {

  val adminWiring: ServerWiring =
    ServerWiring.make(ClusterAwareSettings.onPort(3553).withInterface("eth1"), "csw-location-server")
  LoggingSystemFactory.start("Assembly", "1.0", adminWiring.clusterSettings.hostname, adminWiring.actorSystem)
  adminWiring.locationHttpService.start().await

  import adminWiring.actorRuntime._

  val assemblyActorRef: typed.ActorRef[String] = typedSystem.spawn(behavior, "assembly")
  val componentId                              = ComponentId(Prefix(Subsystem.NFIRAOS, "assembly"), ComponentType.Assembly)
  val connection                               = AkkaConnection(componentId)

  val registration                           = AkkaRegistrationFactory.make(connection, assemblyActorRef.toURI)
  val registrationResult: RegistrationResult = HttpLocationServiceFactory.makeLocalClient.register(registration).await

  def main(args: Array[String]): Unit = {}

  def behavior: Behaviors.Receive[String] = Behaviors.receiveMessage[String] {
    case "Unregister" =>
      AssemblyApp.registrationResult.unregister()
      Behaviors.same
  }
}
