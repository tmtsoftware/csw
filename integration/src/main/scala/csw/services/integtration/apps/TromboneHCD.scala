package csw.services.integtration.apps

import akka.actor.{Actor, Props}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import akka.pattern.pipe
import csw.services.location.common.ActorRuntime
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.scaladsl.LocationService

object TromboneHCD extends App {
  private val actorRuntime = new ActorRuntime("trombone-hcd")

  val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId = ComponentId("trombone-hcd", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  val registration = AkkaRegistration(connection, tromboneHcdActorRef, "nfiraos.ncc.tromboneHCD")
  val registrationResult = LocationService.make(actorRuntime).register(registration).await

}

class TromboneHCD extends Actor {
  import context.dispatcher
  override def receive: Receive = {
    case "Unregister" => TromboneHCD.registrationResult.unregister() pipeTo sender()
  }
}
