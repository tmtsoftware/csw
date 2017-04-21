package csw.services.integtration.apps

import akka.actor.{Actor, ActorSystem, Props}
import csw.services.location.commons.CswCluster
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.test.utils.TestFutureExtension.RichFuture

object TromboneHCD {
  private val cswCluster = CswCluster.make()

  val hcdActorSystem = ActorSystem("trombone-hcd-system")

  val tromboneHcdActorRef = hcdActorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val componentId         = ComponentId("trombonehcd", ComponentType.HCD)
  val connection          = AkkaConnection(componentId)

  val registration            = AkkaRegistration(connection, tromboneHcdActorRef)
  private val locationService = LocationServiceFactory.withCluster(cswCluster)
  val registrationResult      = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {}
  case class Unregister()
}

class TromboneHCD extends Actor {
  import TromboneHCD._
  import cswCluster._

  override def receive: Receive = {
    case Unregister => {
      registrationResult.unregister().onComplete(_ => locationService.shutdown())
    }
  }
}
