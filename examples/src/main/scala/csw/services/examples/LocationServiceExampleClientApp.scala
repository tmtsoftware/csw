package csw.services.examples

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Sink
import csw.services.location.models.ComponentType
import csw.services.location.models.ConnectionType
import csw.services.location.models.{AkkaLocation, LocationRemoved, LocationUpdated}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, LocationServiceFactory}

import scala.concurrent.Await

/**
  * A location service test client application that attempts to resolve one or more
  * akka services.
  * If a command line arg is given, it should be the number of services to resolve (default: 1).
  * The client and service applications can be run on the same or different hosts.
  */
object LocationServiceExampleClientApp extends App {
  private val locationService = LocationServiceFactory.make()
  implicit val system = ActorSystemFactory.remote
  implicit val mat = ActorMaterializer()

  system.actorOf(LocationServiceExampleClient.props(locationService))

}

object LocationServiceExampleClient {

  // message sent when location stream ends (should not happen?)
  case object AllDone

  def props(locationService: LocationService)(implicit mat: Materializer): Props =
    Props(new LocationServiceExampleClient(locationService))
}

/**
  * A test client actor that uses the location service to resolve services
  */
class LocationServiceExampleClient(locationService: LocationService)(implicit mat: Materializer) extends Actor with ActorLogging {

  import LocationServiceExampleClient._
  import scala.concurrent.duration._

  private val timeout = 5.seconds
  private val waitForResolveLimit = 30.seconds

  // find connection to LocationServiceExampleComponent in location service
  // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]
  private val exampleConnection = LocationServiceExampleComponent.connection

  private val findResult = Await.result(locationService.find(exampleConnection), timeout)
  println(s"Find result: $findResult")     // Should be "Find Result: None"

  // resolve connection to LocationServiceExampleComponent
  // [start LocationServiceExampleComponent after this command but before timeout]
  private val resolveResult = Await.result(locationService.resolve(exampleConnection, waitForResolveLimit), waitForResolveLimit+timeout)
  println(s"Resolve result: $resolveResult")

  // list connections in location service
  private val connectionList = Await.result(locationService.list, timeout)

  connectionList.foreach(c => println(s"Location $c"))

  // filter connections based on connection type

  private val componentList = Await.result(locationService.list(ComponentType.Assembly), timeout)

  componentList.foreach(c => println(s"Assembly Location $c"))

  // output should be:

  // filter connections based on component type

  private val akkaList = Await.result(locationService.list(ConnectionType.AkkaType), timeout)

  akkaList.foreach(c => println(s"Akka Location $c"))



  // track connection to LocationServiceExampleComponent
  // Calls track method for example connection and forwards location messages to this actor
  locationService.track(exampleConnection).to(Sink.actorRef(self, AllDone)).run()


  // subscribe to LocationServiceExampleComponent events
  locationService.subscribe(exampleConnection, trackingEvent => {
    log.info("subscription event")
    self ! trackingEvent
  })

  // [tracking shows component unregister and re-register]

  override def receive: Receive = {

    // Receive a location from the location service and if it is an akka location, send it a message
    case LocationUpdated(loc) =>
      log.info(s"Location updated $loc")
      loc match {
        case AkkaLocation(_, _, actorRef) =>
          actorRef ! LocationServiceExampleComponent.ClientMessage
        case x => log.error(s"Received unexpected location type: $x")
      }

    // A location was removed
    case LocationRemoved(conn) =>
      log.info(s"Location removed $conn")

    case x =>
      log.error(s"Received unexpected message $x")
  }

}

