package csw.services.integtration.apps

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TromboneHCD extends App {
  private val actorRuntime = new ActorRuntime("trombone-hcd", 2554)

  val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId = ComponentId("trombonehcd", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  val registration = AkkaRegistration(connection, tromboneHcdActorRef, "nfiraos.ncc.tromboneHCD")
  lazy val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

  println("Trombone HCD registered")
}

class TromboneHCD extends Actor {
  import context.dispatcher
  override def receive: Receive = {
    case "Unregister" => {
      println("Unregistered the connection.")
      TromboneHCD.registrationResult.unregister() pipeTo sender()
    }
    case "Test" => println("Received test message.")
  }
}
