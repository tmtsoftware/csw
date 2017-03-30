package csw.services.integtration.apps

import akka.actor.{Actor, Props}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TromboneHCD {
  private val actorRuntime = new ActorRuntime()

  val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId = ComponentId("trombonehcd", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  val registration = AkkaRegistration(connection, tromboneHcdActorRef)
  private val locationService = LocationServiceFactory.make(actorRuntime)
  val registrationResult = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {

  }
  case class Unregister()
}

class TromboneHCD extends Actor {
  import TromboneHCD._
  import actorRuntime._

  override def receive: Receive = {
    case Unregister => {
      registrationResult.unregister().onComplete(_ => locationService.shutdown())
    }
  }
}
