package csw.services.examples

import akka.actor._
import akka.stream.ActorMaterializer
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, LocationServiceFactory}

/**
  * Starts one or more akka services in order to test the location service.
  * If a command line arg is given, it should be the number of services to start (default: 1).
  * Each service will have a number appended to its name.
  * You should start the TestServiceClient with the same number, so that it
  * will try to find all the services.
  * The client and service applications can be run on the same or different hosts.
  */
object LocationServiceExampleComponentApp extends App {
  private val locationService = LocationServiceFactory.make()
  implicit val system = ActorSystemFactory.remote()
  implicit val mat = ActorMaterializer()

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
  case object ClientMessage
}

/**
  * A dummy akka test service that registers with the location service
  */
class LocationServiceExampleComponent(locationService: LocationService) extends Actor with ActorLogging {

  println("In actor LocationServiceExampleComponent")

  // Register with the location service
  locationService.register(AkkaRegistration(LocationServiceExampleComponent.connection, self))

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
