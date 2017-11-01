package csw.services.location

import java.net.InetAddress

import akka.actor._
import akka.stream.ActorMaterializer
import akka.typed
import akka.typed.Behavior
import akka.typed.scaladsl.adapter._
import csw.messages.RunningMessage.DomainMessage
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.commons.commonlogger.SampleLogger
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, LocationServiceFactory}
import csw.services.logging.internal.LogControlMessages
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * An example that shows how to register a component actor with the location service.
 */
object LocationServiceExampleComponentApp extends App {
  private val locationService         = LocationServiceFactory.make()
  implicit val system: ActorSystem    = ActorSystemFactory.remote()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  //#create-logging-system
  private val host = InetAddress.getLocalHost.getHostName
  LoggingSystemFactory.start("LocationServiceExampleComponent", "0.1", host, system)
  //#create-logging-system

  system.actorOf(LocationServiceExampleComponent.props(locationService))
}

object LocationServiceExampleComponent {
  // Creates the ith service
  def props(locationService: LocationService): Props = Props(new LocationServiceExampleComponent(locationService))

  // Component ID of the service
  val componentId = ComponentId("LocationServiceExampleComponent", ComponentType.Assembly)

  // Connection for the service
  val connection = AkkaConnection(componentId)

  // Message sent from client once location has been resolved
  case object ClientMessage extends DomainMessage
}

/**
 * A dummy akka test service that registers with the location service
 */
class LocationServiceExampleComponent(locationService: LocationService) extends SampleLogger.Actor {

  log.info("In actor LocationServiceExampleComponent")
  val logAdminActorRef: typed.ActorRef[LogControlMessages] =
    ActorSystemFactory.remote().spawn(Behavior.empty, "my-actor-1-admin")
  // Register with the location service
  val registrationResult: Future[RegistrationResult] =
    locationService.register(
      AkkaRegistration(LocationServiceExampleComponent.connection, Some("nfiraos.ncc.trombone"), self, logAdminActorRef)
    )
  Await.result(registrationResult, 5.seconds)

  log.info("LocationServiceExampleComponent registered.")

  override def receive: Receive = {
    // This is the message that TestServiceClient sends when it discovers this service
    case LocationServiceExampleComponent.ClientMessage =>
      log.info(s"Received scala client message from: ${sender()}")

    // This is the message that JTestServiceClient sends when it discovers this service
    //    case m: JTestAkkaService.ClientMessage =>
    //      log.info(s"Received java client message from: ${sender()}")

    case x =>
      log.error(s"Received unexpected message $x")
  }
}
