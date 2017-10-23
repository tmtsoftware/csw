package csw.services.location

import java.net.InetAddress

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.messages.location.Connection.{AkkaConnection, HttpConnection}
import csw.messages.location._
import csw.services.commons.commonlogger.ExampleLogger
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, LocationServiceFactory}
import csw.services.logging.internal.{LogControlMessages, LoggingSystem}
import csw.services.logging.scaladsl.{Keys, LogAdminActorFactory, LoggingSystemFactory}

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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

  implicit val mat: ActorMaterializer = ActorMaterializer()

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
class LocationServiceExampleClient(locationService: LocationService, loggingSystem: LoggingSystem)(
    implicit mat: Materializer
) extends ExampleLogger.Actor
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

  // logAdminActorRef handles dynamically setting/getting log level of the component
  private val logAdminActorRef: ActorRef[LogControlMessages] =
    LogAdminActorFactory.make(context.system)

  // add some dummy registrations for illustrative purposes

  // dummy http connection
  val httpPort                          = 8080
  val httpConnection                    = HttpConnection(ComponentId("configuration", ComponentType.Service))
  val httpRegistration                  = HttpRegistration(httpConnection, httpPort, "path123", logAdminActorRef)
  val httpRegResult: RegistrationResult = Await.result(locationService.register(httpRegistration), 2.seconds)

  // ************************************************************************************************************

  // import scaladsl adapter to implicitly convert UnTyped ActorRefs to Typed ActorRef[Nothing]
  import akka.typed.scaladsl.adapter._

  // dummy HCD connection
  val hcdConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  val hcdRegistration =
    AkkaRegistration(
      hcdConnection,
      context.actorOf(Props(new akka.actor.Actor {
        override def receive: Receive = {
          case "print" => log.info("hello world")
        }
      }), name = "my-actor-1"),
      logAdminActorRef
    )

  // Register UnTyped ActorRef with Location service. Import scaladsl adapter to implicitly convert
  // UnTyped ActorRefs to Typed ActorRef[Nothing]
  val hcdRegResult: RegistrationResult = Await.result(locationService.register(hcdRegistration), 2.seconds)

  // ************************************************************************************************************

  def behavior(): Behavior[String] = Actor.deferred { ctx =>
    Actor.same
  }
  val typedActorRef: ActorRef[String] = context.system.spawn(behavior(), "typed-actor-ref")

  val assemblyConnection = AkkaConnection(ComponentId("assembly1", ComponentType.Assembly))

  // Register Typed ActorRef[String] with Location Service
  val assemblyRegistration                  = AkkaRegistration(assemblyConnection, typedActorRef, logAdminActorRef)
  val assemblyRegResult: RegistrationResult = Await.result(locationService.register(assemblyRegistration), 2.seconds)
  //#Components-Connections-Registrations

  //#find
  // find connection to LocationServiceExampleComponent in location service
  // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]
  val exampleConnection: AkkaConnection = LocationServiceExampleComponent.connection

  //#log-info-map
  log.info(s"Attempting to find $exampleConnection",
           Map(Keys.OBS_ID → "foo_obs_id", "exampleConnection" → exampleConnection.name))
  //#log-info-map
  val findResult: Option[AkkaLocation] = Await.result(locationService.find(exampleConnection), timeout)

  //#log-info
  log.info(s"Result of the find call: $findResult")
  //#log-info
  //#find

  findResult.foreach(akkaLocation ⇒ {
    //#typed-ref
    val typedActorRef: ActorRef[String] = akkaLocation.typedRef[String]
    //#typed-ref
  })
  //#resolve
  // resolve connection to LocationServiceExampleComponent
  // [start LocationServiceExampleComponent after this command but before timeout]
  log.info(s"Attempting to resolve $exampleConnection with a wait of $waitForResolveLimit ...")

  val resolveResultF: Future[Option[AkkaLocation]] = locationService.resolve(exampleConnection, waitForResolveLimit)
  val resolveResult: Option[AkkaLocation]          = Await.result(resolveResultF, waitForResolveLimit + timeout)
  resolveResult match {
    case Some(result) ⇒
      log.info(s"Resolve result: ${locationInfoToString(result)}")
    case None ⇒
      log.info(s"Timeout waiting for location $exampleConnection to resolve.")
  }
  //#resolve

  // example code showing how to get the actorReg for remote component and send it a message
  if (resolveResult.isDefined) {
    resolveResult.get match {
      case c: AkkaLocation =>
        c.typedRef ! LocationServiceExampleComponent.ClientMessage
      case x => log.error(s"Received unexpected location type: $x")
    }
  }

  //#list
  // list connections in location service
  val connectionList: List[Location] = Await.result(locationService.list, timeout)
  log.info("All Registered Connections:")
  connectionList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#list

  //#filtering-component
  // filter connections based on component type
  val componentList: List[Location] = Await.result(locationService.list(ComponentType.Assembly), timeout)
  log.info("Registered Assemblies:")
  componentList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#filtering-component

  //#filtering-connection
  // filter connections based on connection type
  val akkaList: List[Location] = Await.result(locationService.list(ConnectionType.AkkaType), timeout)
  log.info("Registered Akka connections:")
  akkaList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#filtering-connection

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
      log.info(s"Location removed $conn", Map("connection" → conn.toString))

    case AllDone =>
      log.info(s"Tracking of $exampleConnection complete.")

    case x =>
      //#log-error
      val runtimeException = new RuntimeException(s"Received unexpected message $x")
      log.error(runtimeException.getMessage, ex = runtimeException)
    //#log-error
  }

}
