package example.location

import java.net.InetAddress

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SpawnProtocol}
import akka.actor.{Actor, ActorSystem, CoordinatedShutdown, Props, typed}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.typed.scaladsl
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.{ActorMaterializer, Materializer}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.framework.commons.CoordinatedShutdownReasons.ActorTerminatedReason
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models._
import csw.location.wrapper.LocationServerWiring
import csw.logging.api.scaladsl._
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.{Keys, LoggerFactory, LoggingSystemFactory}
import csw.params.core.models.Prefix
import example.location.ExampleMessages.{AllDone, CustomException, TrackingEventAdapter}
import example.location.LocationServiceExampleClient.locationInfoToString
import example.location.LocationServiceExampleClientApp.typedSystem

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * An example location service client application.
 */
object LocationServiceExampleClientApp extends App {

  // http location service client expect that location server is running on local machine
  // here we are starting location http server so that httpLocationClient uses can be illustrated
  private val wiring = new LocationServerWiring().wiring
  Await.result(wiring.locationHttpService.start(), 5.seconds)
  val untypedSystem = ActorSystem("untyped-system")
  //#create-actor-system
  implicit val typedSystem: typed.ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "csw-examples-locationServiceClient")
  //#create-actor-system

  implicit val mat: ActorMaterializer = scaladsl.ActorMaterializer()

  //#create-location-service
  private val locationService = HttpLocationServiceFactory.makeLocalClient(typedSystem, mat)
  //#create-location-service

  //#create-logging-system
  private val host = InetAddress.getLocalHost.getHostName
  // Only call this once per application
  val loggingSystem: LoggingSystem = LoggingSystemFactory.start("LocationServiceExampleClient", "0.1", host, typedSystem)
  //#create-logging-system

  untypedSystem.actorOf(LocationServiceExampleClient.props(locationService, loggingSystem))
}

sealed trait ExampleMessages
object ExampleMessages {
  case class TrackingEventAdapter(trackingEvent: TrackingEvent) extends ExampleMessages
  case class CustomException(throwable: Throwable)              extends ExampleMessages
  case class AllDone(exampleConnection: Connection)             extends ExampleMessages
}

object LocationServiceExampleClient {

  def props(locationService: LocationService, loggingSystem: LoggingSystem)(implicit mat: Materializer): Props =
    Props(new LocationServiceExampleClient(locationService, loggingSystem))

  def locationInfoToString(loc: Location): String = {
    val connection = loc.connection
    s"${connection.name}, component type=${connection.componentId.componentType}, connection type=${connection.connectionType}"
  }

  //#tracking
  def sinkBehavior: Behaviors.Receive[ExampleMessages] = Behaviors.receive[ExampleMessages] { (ctx, msg) =>
    {
      val log: Logger = new LoggerFactory("my-component-name").getLogger(ctx)

      msg match {
        case TrackingEventAdapter(LocationUpdated(loc)) => log.info(s"Location updated ${locationInfoToString(loc)}")
        case TrackingEventAdapter(LocationRemoved(conn)) =>
          log.warn(s"Location removed $conn", Map("connection" -> conn.toString))
        case AllDone(exampleConnection) => log.info(s"Tracking of $exampleConnection complete.")
        case CustomException(throwable) => log.error(throwable.getMessage, ex = throwable)
      }
      Behaviors.same
    }
  }
  //#tracking
}

/**
 * A test client actor that uses the location service to resolve services
 */
class LocationServiceExampleClient(locationService: LocationService, loggingSystem: LoggingSystem)(implicit mat: Materializer)
    extends akka.actor.Actor {

  val log: Logger = new LoggerFactory("my-component-name").getLogger(context)

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

  // ************************************************************************************************************

  // import scaladsl adapter to implicitly convert UnTyped ActorRefs to Typed ActorRef[Nothing]
  import akka.actor.typed.scaladsl.adapter._

  // dummy HCD connection
  val hcdConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  val hcdRegistration: AkkaRegistration = AkkaRegistrationFactory.make(
    hcdConnection,
    Prefix("nfiraos.ncc.tromboneHcd"),
    context
      .actorOf(Props(new Actor {
        override def receive: Receive = {
          case "print" => log.info("hello world")
        }
      }), name = "my-actor-1")
      .toTyped
      .toURI
  )

  // Register UnTyped ActorRef with Location service. Import scaladsl adapter to implicitly convert
  // UnTyped ActorRefs to Typed ActorRef[Nothing]
  val hcdRegResult: RegistrationResult = Await.result(locationService.register(hcdRegistration), 2.seconds)

  // ************************************************************************************************************

  def behavior(): Behavior[String] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage { msg =>
      Behaviors.same
    }
  }
  val typedActorRef: ActorRef[String] = context.system.spawn(behavior(), "typed-actor-ref")

  val assemblyConnection = AkkaConnection(models.ComponentId("assembly1", ComponentType.Assembly))

  // Register Typed ActorRef[String] with Location Service
  val assemblyRegistration: AkkaRegistration =
    AkkaRegistrationFactory.make(assemblyConnection, Prefix("nfiraos.ncc.tromboneAssembly"), typedActorRef.toURI)

  val assemblyRegResult: RegistrationResult = Await.result(locationService.register(assemblyRegistration), 2.seconds)
  //#Components-Connections-Registrations

  //#find
  // find connection to LocationServiceExampleComponent in location service
  // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]
  val exampleConnection: AkkaConnection = LocationServiceExampleComponent.connection

  //#log-info-map
  log.info(
    s"Attempting to find $exampleConnection",
    Map(Keys.OBS_ID -> "foo_obs_id", "exampleConnection" -> exampleConnection.name)
  )
  //#log-info-map
  val findResult: Option[AkkaLocation] = Await.result(locationService.find(exampleConnection), timeout)

  //#log-info
  log.info(s"Result of the find call: $findResult")
  //#log-info
  //#find

  findResult.foreach(akkaLocation => {
    //#typed-ref
    // If the component type is HCD or Assembly, use this to get the correct ActorRef
    val typedComponentRef: ActorRef[ComponentMessage] = akkaLocation.componentRef

    // If the component type is Container, use this to get the correct ActorRef
    val typedContainerRef: ActorRef[ContainerMessage] = akkaLocation.containerRef
    //#typed-ref
  })
  //#resolve
  // resolve connection to LocationServiceExampleComponent
  // [start LocationServiceExampleComponent after this command but before timeout]
  log.info(s"Attempting to resolve $exampleConnection with a wait of $waitForResolveLimit ...")

  val resolveResultF: Future[Option[AkkaLocation]] = locationService.resolve(exampleConnection, waitForResolveLimit)
  val resolveResult: Option[AkkaLocation]          = Await.result(resolveResultF, waitForResolveLimit + timeout)
  resolveResult match {
    case Some(result) =>
      log.info(s"Resolve result: ${locationInfoToString(result)}")
    case None =>
      log.info(s"Timeout waiting for location $exampleConnection to resolve.")
  }
  //#resolve

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

  //#filtering-prefix
  // filter akka locations based on prefix
  val akkaLocations: List[AkkaLocation] = Await.result(locationService.listByPrefix("nfiraos.ncc"), timeout)
  log.info("Registered akka locations for nfiraos.ncc")
  akkaLocations.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  //#filtering-prefix

  if (resolveResult.isDefined) {

    //#tracking
    // the following two methods are examples of two ways to track a connection.
    // both are implemented but only one is really needed.

    // Method1: track connection to LocationServiceExampleComponent
    // Calls track method for example connection and forwards location messages to this actor
    //
    log.info(s"Starting to track $exampleConnection")
    val sinfActorRef = typedSystem.spawn(LocationServiceExampleClient.sinkBehavior, "")
    locationService
      .track(exampleConnection)
      .map(TrackingEventAdapter)
      .to(ActorSink.actorRef[ExampleMessages](sinfActorRef, AllDone(exampleConnection), CustomException))
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

    // Gracefully shutdown actor system
    Await.result(CoordinatedShutdown(context.system).run(ActorTerminatedReason), 20.seconds)

    //#stop-logging-system
    // Only call this once per application
    Await.result(loggingSystem.stop, 30.seconds)
    //#stop-logging-system
  }

  override def receive: Receive = {
    case x =>
    // Receive a location from the location service and if it is an akka location, send it a message
    case LocationUpdated(loc) =>
      log.info(s"Location updated ${locationInfoToString(loc)}")

    // A location was removed
    case LocationRemoved(conn) =>
      log.info(s"Location removed $conn", Map("connection" -> conn.toString))

    case AllDone =>
      log.info(s"Tracking of $exampleConnection complete.")

    case x =>
      //#log-error
      val runtimeException = new RuntimeException(s"Received unexpected message $x")
      log.error(runtimeException.getMessage, ex = runtimeException)
    //#log-error

  }
}
