package csw.integtration.apps

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import csw.clusterseed.client.HTTPLocationService
import csw.command.messages.CommandMessage.Submit
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaRegistration, ComponentId, ComponentType, RegistrationResult}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix

import scala.concurrent.ExecutionContext

object TromboneHCD {
  implicit val hcdActorSystem: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer      = ActorMaterializer()
  implicit val ec: ExecutionContext        = hcdActorSystem.toTyped.executionContext

  val tromboneHcdActorRef: ActorRef = hcdActorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId                   = ComponentId("trombonehcd", ComponentType.HCD)
  val connection                    = AkkaConnection(componentId)

  val registration                           = AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), tromboneHcdActorRef)
  private val locationService                = HttpLocationServiceFactory.makeLocalClient
  val registrationResult: RegistrationResult = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {}
}

class TromboneHCD extends Actor with HTTPLocationService {
  import TromboneHCD._

  override def receive: Receive = {
    case Submit(Setup(_, _, CommandName("Unregister"), None, _), _) ⇒
      registrationResult.unregister().onComplete(_ => super.afterAll())
  }
}
