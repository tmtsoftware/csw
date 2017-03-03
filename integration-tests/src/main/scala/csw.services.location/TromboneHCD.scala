package csw.services.location.integration
import akka.actor.{Actor, Props}
import csw.services.location.common.ActorRuntime
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType, RegistrationResult}
import csw.services.location.scaladsl.LocationService


object TromboneHCD {
  private val PORT : Int = 2553
  private val actorRuntime = new ActorRuntime("trombone-hcd", PORT)
  private var registration : AkkaRegistration = _

  private var registrationResult : RegistrationResult = _

  def main(args: Array[String]): Unit = {

    val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
    val componentId = ComponentId("trombone-hcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)

    registration = AkkaRegistration(connection, tromboneHcdActorRef, "nfiraos.ncc.tromboneHCD")
    registrationResult = LocationService.make(actorRuntime).register(registration).await
  }
}

class TromboneHCD extends Actor {
  override def receive: Receive = {
    case "Unregister" => TromboneHCD.registrationResult.unregister()
  }
}
