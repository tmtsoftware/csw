package csw.services.integtration.apps

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.typed.scaladsl.adapter._
import csw.messages.models.location.Connection.AkkaConnection
import csw.messages.models.location.{ComponentId, ComponentType}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.CswCluster
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.LocationServiceFactory

object TromboneHCD {
  private val cswCluster = CswCluster.make()

  val hcdActorSystem = ActorSystem("trombone-hcd-system")

  val tromboneHcdActorRef: ActorRef = hcdActorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId                   = ComponentId("trombonehcd", ComponentType.HCD)
  val connection                    = AkkaConnection(componentId)

  val registration                           = AkkaRegistration(connection, tromboneHcdActorRef)
  private val locationService                = LocationServiceFactory.withCluster(cswCluster)
  val registrationResult: RegistrationResult = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {}
  case class Unregister()
}

class TromboneHCD extends Actor {
  import TromboneHCD._
  import cswCluster._

  override def receive: Receive = {
    case Unregister => registrationResult.unregister().onComplete(_ => locationService.shutdown())
  }
}
