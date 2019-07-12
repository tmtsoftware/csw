package csw.integtration.apps

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.CommandMessage.Submit
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.RegistrationResult
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{AkkaRegistration, ComponentId, ComponentType}
import csw.location.server.internal.ServerWiring
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix

object TromboneHCD {

  val adminWiring: ServerWiring = ServerWiring.make(Some(3553))
  LoggingSystemFactory.start("Assembly", "1.0", adminWiring.clusterSettings.hostname, adminWiring.actorSystem)

  adminWiring.locationHttpService.start().await

  import adminWiring.actorRuntime._

  val tromboneHcdActorRef: ActorRef[Submit] = typedSystem.spawn(behavior, "trombone-hcd")
  val componentId                           = ComponentId("trombonehcd", ComponentType.HCD)
  val connection                            = AkkaConnection(componentId)

  val registration                           = AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), tromboneHcdActorRef.toURI)
  private val locationService                = HttpLocationServiceFactory.makeLocalClient
  val registrationResult: RegistrationResult = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {}

  def behavior: Behaviors.Receive[Submit] = Behaviors.receiveMessage[Submit] {
    case Submit(Setup(_, _, CommandName("Unregister"), None, _), _) ⇒
      registrationResult.unregister()
      Behaviors.same
    case x ⇒
      println(s"Trombone HCD received [$x]")
      Behaviors.same
  }
}
