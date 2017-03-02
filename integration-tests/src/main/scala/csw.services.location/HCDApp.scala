package csw.services.location.integration
import akka.actor.{Actor, Props}
import csw.services.location.common.ActorRuntime
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType, RegistrationResult}
import csw.services.location.scaladsl.LocationService


object HCDApp {
  private val actorRuntime = new ActorRuntime("AssemblySystem", 2553)
  private var registration : AkkaRegistration = _
  private var future: RegistrationResult = _

  def main(args: Array[String]): Unit = {

    val hcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "hcd1")
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)

    registration = AkkaRegistration(connection, hcdActorRef, "nfiraos.ncc.tromboneHCD")
    future = LocationService.make(actorRuntime).register(registration).await

    hcdActorRef ! "SUCEESS"

  }
}

class TromboneHCD extends Actor {
  override def receive: Receive = {
    case _ => println("Trombone HCD is registered to JmDns.")
  }
}
