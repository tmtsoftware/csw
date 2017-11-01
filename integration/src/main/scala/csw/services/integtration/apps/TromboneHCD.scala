package csw.services.integtration.apps

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.typed
import akka.typed.Behavior
import akka.typed.scaladsl.adapter._
import csw.messages.RunningMessage.DomainMessage
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.CswCluster
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.internal.LogControlMessages

object TromboneHCD {
  private val cswCluster = CswCluster.make()

  val hcdActorSystem = ActorSystem("trombone-hcd-system")

  val tromboneHcdActorRef: ActorRef                        = hcdActorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
  val logAdminActorRef: typed.ActorRef[LogControlMessages] = hcdActorSystem.spawn(Behavior.empty, "trombone-hcd-admin")
  val componentId                                          = ComponentId("trombonehcd", ComponentType.HCD)
  val connection                                           = AkkaConnection(componentId)

  val registration                           = AkkaRegistration(connection, Some("nfiraos.ncc.trombone"), tromboneHcdActorRef, logAdminActorRef)
  private val locationService                = LocationServiceFactory.withCluster(cswCluster)
  val registrationResult: RegistrationResult = locationService.register(registration).await

  println("Trombone HCD registered")

  def main(args: Array[String]): Unit = {}
  case class Unregister() extends DomainMessage
}

class TromboneHCD extends Actor {
  import TromboneHCD._
  import cswCluster._

  override def receive: Receive = {
    case Unregister => registrationResult.unregister().onComplete(_ => locationService.shutdown())
  }
}
