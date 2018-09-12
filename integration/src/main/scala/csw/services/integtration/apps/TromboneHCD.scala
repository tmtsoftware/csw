package csw.services.integtration.apps

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import csw.messages.CommandMessage.Submit
import csw.messages.commands.{CommandName, Setup}
import csw.services.location.api.models.Connection.AkkaConnection
import csw.services.location.api.models.{ComponentId, ComponentType}
import csw.messages.params.models.Prefix
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.api.models.AkkaRegistration
import csw.services.location.models.RegistrationResult
import csw.services.location.scaladsl.LocationServiceFactory

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
