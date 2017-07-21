package csw.services.location

import java.net.InetAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.commons.ExampleLogger
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, LocationServiceFactory}
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.{Keys, LoggingSystemFactory}

import scala.async.Async._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * An example location service client application.
 */
object LocationServiceExampleClientApp extends App {

  //#create-location-service
  private val locationService = LocationServiceFactory.make()
  //#create-location-service

  //#create-actor-system
  implicit val system: ActorSystem =
    ActorSystemFactory.remote("csw-examples-locationServiceClient")
  //#create-actor-system

  implicit val mat = ActorMaterializer()

  //#create-logging-system
  private val host = InetAddress.getLocalHost.getHostName
  // Only call this once per application
  val loggingSystem: LoggingSystem = LoggingSystemFactory.start("LocationServiceExampleClient", "0.1", host, system)
  //#create-logging-system

  system.actorOf(LocationServiceExampleClient.props(locationService, loggingSystem))
}

object LocationServiceExampleClient {

  // message sent when location stream ends (should not happen?)
  case object AllDone

  def props(locationService: LocationService, loggingSystem: LoggingSystem)(implicit mat: Materializer): Props =
    Props(new LocationServiceExampleClient(locationService, loggingSystem))
}

/**
 * A test client actor that uses the location service to resolve services
 */
//#actor-mixin
class LocationServiceExampleClient(locationService: LocationService,
                                   loggingSystem: LoggingSystem)(implicit mat: Materializer)
    extends ExampleLogger.Actor
    //#actor-mixin
    {
  import LocationServiceExampleClient._

  private val timeout             = 5.seconds
  private val waitForResolveLimit = 30.seconds

  // EXAMPLE DEMO START

  // This demo shows some basics of using the location service.  Before running this example,
  // optionally use the location service csw-location-agent to start a redis server:
  //
  //   $ csw-location-agent --name redis --command "redis-server --port %port"
  //
  // Not only does this serve as an example for starting applications that are not written in CSW,
  // but it also will help demonstrate location filtering later in this demo.

  //#Components-Connections-Registrations

  // add some dummy registrations for illustrative purposes

  // dummy http connection
  val httpPort                          = 8080
  val httpConnection                    = HttpConnection(ComponentId("configuration", ComponentType.Service))
  val httpRegistration                  = HttpRegistration(httpConnection, httpPort, "path123")
  val httpRegResult: RegistrationResult = Await.result(locationService.register(httpRegistration), 2.seconds)

  // dummy HCD connection
  val hcdConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  val hcdRegistration =
    AkkaRegistration(hcdConnection, context.actorOf(Props(new Actor {
      override def receive: Receive = {
        case "print" => log.info("hello world")
      }
    }), name = "my-actor-1"))
  val hcdRegResult: RegistrationResult = Await.result(locationService.register(hcdRegistration), 2.seconds)

  //register the client "assembly" created in this example
  val assemblyConnection                    = AkkaConnection(ComponentId("assembly1", ComponentType.Assembly))
  val assemblyRegistration                  = AkkaRegistration(assemblyConnection, self)
  val assemblyRegResult: RegistrationResult = Await.result(locationService.register(assemblyRegistration), 2.seconds)
  //#Components-Connections-Registrations

  //#find
  // find connection to LocationServiceExampleComponent in location service
  // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]
  val exampleConnection: AkkaConnection = LocationServiceExampleComponent.connection

  //#log-info-map
  log.info("Attempting to find connection",
           Map(Keys.OBS_ID → "foo_obs_id", "exampleConnection" → exampleConnection.name))
  //#log-info-map
  val findResult: Option[Location] = Await.result(locationService.find(exampleConnection), timeout)

  //#log-info
  log.info(s"Result of the find call: $findResult")
  //#log-info
  //#find

  // Output should be:
  //    Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) ...
  //    Find result: None

  //#resolve
  // resolve connection to LocationServiceExampleComponent
  // [start LocationServiceExampleComponent after this command but before timeout]
  log.info(s"Attempting to resolve $exampleConnection with a wait of $waitForResolveLimit ...")

  val resolveResultF: Future[Option[Location]] = locationService.resolve(exampleConnection, waitForResolveLimit)
  val resolveResult: Option[Location]          = Await.result(resolveResultF, waitForResolveLimit + timeout)
  resolveResult match {
    case Some(result) ⇒
      log.info(s"Resolve result: ${locationInfoToString(result)}")
    case None ⇒
      log.info(s"Timeout waiting for location $exampleConnection to resolve.")
  }
  //#resolve

  // Output should be:
  //    Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...

  // If you then start the LocationServiceExampleComponentApp,
  // Output should be:
  //    Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  // If not,
  // Output should be:
  //    Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.

  // example code showing how to get the actorReg for remote component and send it a message
  if (resolveResult.isDefined) {
    resolveResult.get match {
      case AkkaLocation(_, _, actorRef) =>
        actorRef ! LocationServiceExampleComponent.ClientMessage
      case x => log.error(s"Received unexpected location type: $x")
    }
  }

  //#list
  // list connections in location service
  val connectionList: List[Location] = Await.result(locationService.list, timeout)
  log.info("All Registered Connections:")
  connectionList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#list

  // Output should be:
  //    All Registered Connections:
  //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
  //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
  //    --- redis-service-tcp, component type=Service, connection type=TcpType
  //    --- configuration-service-http, component type=Service, connection type=HttpType
  //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  //#filtering-component
  // filter connections based on component type
  val componentList: List[Location] = Await.result(locationService.list(ComponentType.Assembly), timeout)
  log.info("Registered Assemblies:")
  componentList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#filtering-component

  // Output should be:
  //    Registered Assemblies:
  //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
  //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  //#filtering-connection
  // filter connections based on connection type
  val akkaList: List[Location] = Await.result(locationService.list(ConnectionType.AkkaType), timeout)
  log.info("Registered Akka connections:")
  akkaList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#filtering-connection

  // Output should be:
  //    Registered Akka connections:
  //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
  //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
  //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

  if (resolveResult.isDefined) {

    //#tracking
    // the following two methods are examples of two ways to track a connection.
    // both are implemented but only one is really needed.

    // Method1: track connection to LocationServiceExampleComponent
    // Calls track method for example connection and forwards location messages to this actor
    //
    log.info(s"Starting to track $exampleConnection")
    locationService
      .track(exampleConnection)
      .to(Sink.actorRef(self, AllDone))
      .run()
    //track returns a Killswitch, that can be used to turn off notifications arbitarily
    //in this case track a connection for 5 seconds, after that schedule switching off the stream
    val killswitch = locationService
      .track(httpConnection)
      .toMat(Sink.foreach(println))(Keep.left)
      .run()
    context.system.scheduler.scheduleOnce(5.seconds) {
      killswitch.shutdown()
    }

    // Method2: subscribe to LocationServiceExampleComponent events
    log.info(s"Starting a subscription to $exampleConnection")
    locationService.subscribe(
      exampleConnection,
      trackingEvent => {
        // the following println is to distinguish subscription events from tracking events
        log.info("subscription event")
        self ! trackingEvent
      }
    )
    //#tracking

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

    // scalastyle:on print.ln

  }

  def locationInfoToString(loc: Location): String = {
    val connection = loc.connection
    s"${connection.name}, component type=${connection.componentId.componentType}, connection type=${connection.connectionType}"
  }

  // Note: This method is never actually called in this example, since the actor does not shutdown
  // unless the application is killed. It is here to demonstrate how you might cleanly shutdown
  // tha application.
  override def postStop(): Unit = {

    //#unregister
    val unregisterF = async {
      httpRegResult.unregister()
      hcdRegResult.unregister()
      assemblyRegResult.unregister()
    }
    Await.result(unregisterF, 5.seconds)
    //#unregister

    //#shutdown
    // Only call this once per application
    Await.result(locationService.shutdown(), 20.seconds)
    //#shutdown

    //#stop-logging-system
    // Only call this once per application
    Await.result(loggingSystem.stop, 30.seconds)
    //#stop-logging-system
  }

  override def receive: Receive = {

    // Receive a location from the location service and if it is an akka location, send it a message
    case LocationUpdated(loc) =>
      log.info(s"Location updated ${locationInfoToString(loc)}")

    // A location was removed
    case LocationRemoved(conn) =>
      log.info("Location removed", Map("connection" → conn.toString))

    case AllDone =>
      log.info(s"Tracking of $exampleConnection complete.")

    case x =>
      val runtimeException = new RuntimeException(s"Received unexpected message $x")
      //#log-error
      log.error(runtimeException.getMessage, ex = runtimeException)
    //#log-error
  }

}
