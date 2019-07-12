package csw.integtration.apps

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.RegistrationResult
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{AkkaRegistration, ComponentId, ComponentType}
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.internal.ServerWiring
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.core.models.Prefix

object AssemblyApp {

  val adminWiring: ServerWiring = ServerWiring.make(ClusterAwareSettings.onPort(3553).withInterface("eth1"))
  LoggingSystemFactory.start("Assembly", "1.0", adminWiring.clusterSettings.hostname, adminWiring.actorSystem)
  adminWiring.locationHttpService.start().await

  import adminWiring.actorRuntime._

  val assemblyActorRef: typed.ActorRef[String] = typedSystem.spawn(behavior, "assembly")
  val componentId                              = ComponentId("assembly", ComponentType.Assembly)
  val connection                               = AkkaConnection(componentId)

  val registration                           = AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), assemblyActorRef.toURI)
  val registrationResult: RegistrationResult = HttpLocationServiceFactory.makeLocalClient.register(registration).await

  def main(args: Array[String]): Unit = {}

  def behavior: Behaviors.Receive[String] = Behaviors.receiveMessage[String] {
    case "Unregister" ⇒
      AssemblyApp.registrationResult.unregister()
      Behaviors.same
  }
}
