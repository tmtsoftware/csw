package csw.services.location.integration
import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType, RegistrationResult}
import csw.services.location.scaladsl.LocationService


object HCD1App extends App {
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2552").
    withFallback(ConfigFactory.load())
  val actorSystem = ActorSystem("HCDSystem", config)

  val hcdActorRef = actorSystem.actorOf(Props[HCD1], "hcd1")
  val componentId = ComponentId("hcd1", ComponentType.HCD)
  val connection = AkkaConnection(componentId)

  private val registration = AkkaRegistration(connection, hcdActorRef, "nfiraos.ncc.tromboneHCD")
  private val future: RegistrationResult = LocationService.make(actorSystem).register(registration).await

  hcdActorRef ! "Move"

}

class HCD1 extends Actor {
  override def receive: Receive = {
    case _ => println("Moved")
  }
}
