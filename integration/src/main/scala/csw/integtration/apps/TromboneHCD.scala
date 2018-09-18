package csw.integtration.apps

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import csw.command.messages.CommandMessage.Submit
import csw.params.commands.{CommandName, Setup}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.core.models.Prefix
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.commons.ClusterAwareSettings
import csw.location.api.models.AkkaRegistration
import csw.location.api.models.RegistrationResult
import csw.location.scaladsl.LocationServiceFactory

import scala.concurrent.ExecutionContextExecutor

object TromboneHCD {
  val hcdActorSystem: ActorSystem           = ClusterAwareSettings.system
  implicit val ec: ExecutionContextExecutor = hcdActorSystem.toTyped.executionContext

  val tromboneHcdActorRef: ActorRef = hcdActorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId                   = ComponentId("trombonehcd", ComponentType.HCD)
  val connection                    = AkkaConnection(componentId)

  val registration                           = AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), tromboneHcdActorRef, null)
  private val locationService                = LocationServiceFactory.withSystem(hcdActorSystem)
  val registrationResult: RegistrationResult = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {}
}

class TromboneHCD extends Actor {
  import TromboneHCD._

  override def receive: Receive = {
    case Submit(Setup(_, _, CommandName("Unregister"), None, _), _) ⇒
      registrationResult.unregister().onComplete(_ => locationService.shutdown(UnknownReason))
  }
}
