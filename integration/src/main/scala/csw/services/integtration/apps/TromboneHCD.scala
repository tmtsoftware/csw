package csw.services.integtration.apps

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.models.Connection.AkkaConnection
import csw.services.location.scaladsl.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TromboneHCD extends App {
  private val actorRuntime = new ActorRuntime("trombone-hcd")

  val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId = ComponentId("trombonehcd", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  val registration = AkkaRegistration(connection, tromboneHcdActorRef, "nfiraos.ncc.tromboneHCD")
  val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

  println("Trombone HCD registered")
}

class TromboneHCD extends Actor {
  import context.dispatcher
  override def receive: Receive = {
    case "Unregister" => TromboneHCD.registrationResult.unregister() pipeTo sender()
  }
}
