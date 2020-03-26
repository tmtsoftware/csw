package csw.integtration.apps

import akka.actor.typed.scaladsl.Behaviors
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.internal.ServerWiring
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS

object AssemblyApp extends App {

  private val locationWiring = ServerWiring.make(ClusterAwareSettings.onPort(3553).withInterface("eth1"), enableAuth = false)
  locationWiring.actorRuntime.startLogging("Assembly", locationWiring.clusterSettings.hostname)
  locationWiring.locationHttpService.start().await

  import locationWiring.actorRuntime._

  private val assemblyActorRef = typedSystem.spawn(behavior, "assembly")
  private val componentId      = ComponentId(Prefix(NFIRAOS, "assembly"), Assembly)
  private val connection       = AkkaConnection(componentId)

  private val registration       = AkkaRegistrationFactory.make(connection, assemblyActorRef.toURI)
  private val locationService    = HttpLocationServiceFactory.makeLocalClient
  private val registrationResult = locationService.register(registration).await

  def behavior: Behaviors.Receive[String] = Behaviors.receiveMessage[String] {
    case "Unregister" =>
      registrationResult.unregister()
      Behaviors.same
  }
}
