package csw.services.examples

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Sink
import scala.concurrent.duration._
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models._
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
  implicit val system = ActorSystemFactory.remote()
  implicit val mat = ActorMaterializer()

  // create an actor ref to use for a dummy HCD registration
  private val dummyActorRef = system.actorOf(Props(new Actor {
    override def receive: Receive = {
      case "print" => println("hello world")
    }
  }), name="my-actor-1")
  system.actorOf(LocationServiceExampleClient.props(locationService))

  private val timeout = 5.seconds

  // add some dummy registrations for illustrative purposes
  private val httpConnection   = HttpConnection(ComponentId("configuration", ComponentType.Service))
  private val httpRegistration = HttpRegistration(httpConnection, 8080, "path123")
  private val httpRegResult = Await.result(locationService.register(httpRegistration), timeout)

  private val hcdConnection   = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  private val hcdRegistration = AkkaRegistration(hcdConnection, dummyActorRef)
  private val hcdRegResult = Await.result(locationService.register(hcdRegistration), timeout)
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

  private val timeout = 5.seconds
  private val waitForResolveLimit = 30.seconds




  // EXAMPLE DEMO START

  // This demo shows some basics of using the location service.  Before running this example,
  // optionally use the location service csw-location-agent to start a redis server:
  //
  //   $ csw-location-agent --name redis --command "redis-server --port %port"
  //
  // Not only does this serve as an example for starting applications that are not written in CSW,
  // but it also will help demonstrate location filtering later in this demo.

  // find connection to LocationServiceExampleComponent in location service
  // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]
  private val exampleConnection = LocationServiceExampleComponent.connection

  println(s"Attempting to find connection $exampleConnection ...")
  private val findResult = Await.result(locationService.find(exampleConnection), timeout)
  println(s"Find result: $findResult")

  // Output should be:
  //    Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) ...
  //    Find result: None

  // resolve connection to LocationServiceExampleComponent
  // [start LocationServiceExampleComponent after this command but before timeout]
  println(s"Attempting to resolve $exampleConnection with a wait of $waitForResolveLimit ...")
  private val resolveResult = Await.result(locationService.resolve(exampleConnection, waitForResolveLimit), waitForResolveLimit + timeout)
  if (resolveResult.isEmpty) {
    println(s"Timeout waiting for location $exampleConnection to resolve.")
  } else {
    println(s"Resolve result: ${locationInfoToString(resolveResult.get)}")
  }

  // Output should be:
  //    Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...

  // If you then start the LocationServiceExampleComponentApp,
  // Output should be:
  //    Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  // If not,
  // Output should be:
  //    Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.

  // list connections in location service
  private val connectionList = Await.result(locationService.list, timeout)
  println("All Registered Connections:")
  connectionList.foreach(c => println(s"--- ${locationInfoToString(c)}"))

  // Output should be:
  //    All Registered Connections:
  //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
  //    --- redis-service-tcp, component type=Service, connection type=TcpType
  //    --- configuration-service-http, component type=Service, connection type=HttpType
  //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType


  // filter connections based on connection type
  private val componentList = Await.result(locationService.list(ComponentType.Assembly), timeout)
  println("Registered Assemblies:")
  componentList.foreach(c => println(s"--- ${locationInfoToString(c)}"))

  // Output should be:
  //    Registered Assemblies:
  //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  // filter connections based on component type
  private val akkaList = Await.result(locationService.list(ConnectionType.AkkaType), timeout)
  println("Registered Akka connections:")
  akkaList.foreach(c => println(s"--- ${locationInfoToString(c)}"))

  // Output should be:
  //    Registered Akka connections:
  //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
  //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  if (resolveResult.isDefined) {
    // the following two methods are examples of two ways to track a connection.
    // both are implemented but only one is really needed.


    // track connection to LocationServiceExampleComponent
    // Calls track method for example connection and forwards location messages to this actor
    println(s"Starting to track $exampleConnection")
    locationService.track(exampleConnection).to(Sink.actorRef(self, AllDone)).run()

    // subscribe to LocationServiceExampleComponent events
    println(s"Starting a subscription to $exampleConnection")
    locationService.subscribe(exampleConnection, trackingEvent => {
      // the following println is to distinguish subscription events from tracking events
      println("subscription event")
      self ! trackingEvent
    })

    // [tracking shows component unregister and re-register]



    // Output should be:
    //    Starting to track AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
    //    Starting a subscription to AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
    //    subscription event
    //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
    //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

    // If you now stop the LocationServiceExampleComponentApp,
    // Output should be:
    //    subscription event
    //    Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
    //    Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))

    // If you start the LocationServiceExampleComponentApp again,
    // Output should be:
    //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
    //    subscription event
    //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType


  }
  def locationInfoToString(loc: Location): String = {
    val connection = loc.connection
    s"${connection.name}, component type=${connection.componentId.componentType}, connection type=${connection.connectionType}"
  }

  override def receive: Receive = {

    // Receive a location from the location service and if it is an akka location, send it a message
    case LocationUpdated(loc) =>
      println(s"Location updated ${locationInfoToString(loc)}")
      loc match {
        case AkkaLocation(_, _, actorRef) =>
          actorRef ! LocationServiceExampleComponent.ClientMessage
        case x => log.error(s"Received unexpected location type: $x")
      }

    // A location was removed
    case LocationRemoved(conn) =>
      println(s"Location removed $conn")

    case x =>
      log.error(s"Received unexpected message $x")
  }

}

