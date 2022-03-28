/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.location

import java.net.InetAddress

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SpawnProtocol}
import akka.actor.{Actor, ActorSystem, Props, typed}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.typed.scaladsl.ActorSink
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.location.api
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.*
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.wrapper.LocationServerWiring
import csw.logging.api.scaladsl.*
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.{Keys, LoggerFactory, LoggingSystemFactory}
import csw.prefix.models.{Prefix, Subsystem}
import example.location.ExampleMessages.{AllDone, CustomException, TrackingEventAdapter}
import example.location.LocationServiceExampleClient.locationInfoToString

import scala.annotation.nowarn
import scala.async.Async.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
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
  // #create-actor-system
  val typedSystem: typed.ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "csw-examples-locationServiceClient")
  // #create-actor-system

  // #create-location-service
  private val locationService = HttpLocationServiceFactory.makeLocalClient(typedSystem)
  // #create-location-service

  // #create-logging-system
  private val host = InetAddress.getLocalHost.getHostName
  // Only call this once per application
  val loggingSystem: LoggingSystem = LoggingSystemFactory.start("LocationServiceExampleClient", "0.1", host, typedSystem)
  // #create-logging-system

  untypedSystem.actorOf(LocationServiceExampleClient.props(locationService, loggingSystem)(typedSystem))
}

sealed trait ExampleMessages
object ExampleMessages {
  case class TrackingEventAdapter(trackingEvent: TrackingEvent) extends ExampleMessages
  case class CustomException(throwable: Throwable)              extends ExampleMessages
  case class AllDone(exampleConnection: Connection)             extends ExampleMessages
}

object LocationServiceExampleClient {

  def props(locationService: LocationService, loggingSystem: LoggingSystem)(implicit
      system: typed.ActorSystem[SpawnProtocol.Command]
  ): Props =
    Props(new LocationServiceExampleClient(locationService, loggingSystem))

  def locationInfoToString(loc: Location): String = {
    val connection = loc.connection
    s"${connection.name}, component type=${connection.componentId.componentType}, connection type=${connection.connectionType}"
  }

  // #tracking
  def sinkBehavior: Behaviors.Receive[ExampleMessages] =
    Behaviors.receive[ExampleMessages] { (ctx, msg) =>
      {
        val log: Logger = new LoggerFactory(Prefix("csw.my-component-name")).getLogger(ctx)

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
  // #tracking
}

/**
 * A test client actor that uses the location service to resolve services
 */
class LocationServiceExampleClient(locationService: LocationService, loggingSystem: LoggingSystem)(implicit
    typedSystem: typed.ActorSystem[SpawnProtocol.Command]
) extends akka.actor.Actor {

  val log: Logger = new LoggerFactory(Prefix("csw.my-component-name")).getLogger(context)

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

  // #Components-Connections-Registrations

  // add some dummy registrations for illustrative purposes

  // dummy http connection
  val httpPort = 8080
  val httpConnection: HttpConnection = HttpConnection(
    api.models.ComponentId(Prefix(Subsystem.CSW, "configuration"), ComponentType.Service)
  )

  // When no network type is provided in httpRegistration, default is NetworkType.Private
  val httpRegistration: HttpRegistration = HttpRegistration(httpConnection, httpPort, "path123")
  private val httpRegResultF: Future[RegistrationResult] = locationService
    .register(httpRegistration)
  httpRegResultF
    .map(httpRegResult => {
      log.info(s"$httpRegResult successfully registered in location service")
    })
  // When a service wants to register itself on Public network, it can provide NetworkType.Public in httpRegistration
  val httpRegistrationOnPublicNetwork: HttpRegistration =
    HttpRegistration(httpConnection, httpPort, "path123", NetworkType.Outside)
  private val httpRegResultOnPublicNetworkF: Future[RegistrationResult] =
    locationService.register(httpRegistrationOnPublicNetwork)
  httpRegResultOnPublicNetworkF
    .map(httpRegResultOnPublicNetwork => {
      log.info(s"$httpRegResultOnPublicNetwork successfully registered in location service")
    })
  // ************************************************************************************************************

  // import scaladsl adapter to implicitly convert UnTyped ActorRefs to Typed ActorRef[Nothing]
  import akka.actor.typed.scaladsl.adapter.*

  // dummy HCD connection
  val hcdConnection: AkkaConnection = AkkaConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
  val hcdRegistration: AkkaRegistration = AkkaRegistrationFactory.make(
    hcdConnection,
    context
      .actorOf(
        Props(new Actor {
          override def receive: Receive = { case "print" =>
            log.info("hello world")
          }
        }),
        name = "my-actor-1"
      )
      .toTyped
  )

  // Register UnTyped ActorRef with Location service. Import scaladsl adapter to implicitly convert
  // UnTyped ActorRefs to Typed ActorRef[Nothing]
  private val hcdRegResultF: Future[RegistrationResult] = locationService
    .register(hcdRegistration)
  hcdRegResultF
    .map(hcdRegResult => {
      log.info(s"$hcdRegResult successfully registered in location service")
    })
  // ************************************************************************************************************

  def behavior(): Behavior[String]    = Behaviors.setup { _ => Behaviors.receiveMessage { _ => Behaviors.same } }
  val typedActorRef: ActorRef[String] = context.system.spawn(behavior(), "typed-actor-ref")

  val assemblyConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.NFIRAOS, "assembly1"), ComponentType.Assembly)
  )

  // Register Typed ActorRef[String] with Location Service
  val assemblyRegistration: AkkaRegistration = AkkaRegistrationFactory.make(assemblyConnection, typedActorRef)

  private val assemblyRegResultF: Future[RegistrationResult] = locationService
    .register(assemblyRegistration)
  assemblyRegResultF
    .map(assemblyRegResult => {
      log.info(s"$assemblyRegResult successfully registered in location service")
    })
  // #Components-Connections-Registrations

  // #Components-Connections-Registrations-With-Metadata
  // add some dummy registrations for illustrative purposes

  // dummy http connection
  val httpPortForService = 8080
  val httpConnectionForService: HttpConnection = HttpConnection(
    api.models.ComponentId(Prefix(Subsystem.CSW, "configuration"), ComponentType.Service)
  )

  // When no network type is provided in httpRegistration, default is NetworkType.Private
  val httpRegistrationForService: HttpRegistration =
    HttpRegistration(httpConnectionForService, httpPortForService, "path123", Metadata(Map("key1" -> "value1")))
  private val httpRegResultForServiceF: Future[RegistrationResult] = locationService
    .register(httpRegistrationForService)

  httpRegResultForServiceF
    .map(httpRegResultForService => {
      log.info(s"$httpRegResultForService successfully registered in location service")
    })
  // #Components-Connections-Registrations-With-Metadata

  // #find
  // find connection to LocationServiceExampleComponent in location service
  // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]
  val exampleConnection: AkkaConnection = LocationServiceExampleComponent.connection

  // #log-info-map
  log.info(
    s"Attempting to find $exampleConnection",
    Map(Keys.OBS_ID -> "foo_obs_id", "exampleConnection" -> exampleConnection.name)
  )
  // #log-info-map
  locationService.find(exampleConnection).map {
    case Some(findResult) =>
      // #log-info
      log.info(s"Result of the find call: $findResult")
      // #log-info
      val akkaLocation = findResult
      // #typed-ref
      // If the component type is HCD or Assembly, use this to get the correct ActorRef
      val typedComponentRef: ActorRef[ComponentMessage] = akkaLocation.componentRef

      // If the component type is Container, use this to get the correct ActorRef
      val typedContainerRef: ActorRef[ContainerMessage] = akkaLocation.containerRef
    // #typed-ref
    case None => // do something when nothing is found
  }

  // #find

  // #resolve
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
  // #resolve

  // #list
  // list connections in location service
  locationService.list.map(connectionList => {
    log.info("All Registered Connections:")
    connectionList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
  })

  // #list

  // #filtering-component
  // filter connections based on component type
  locationService
    .list(ComponentType.Assembly)
    .map(componentList => {
      log.info("Registered Assemblies:")
      componentList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
    })
  // #filtering-component

  // #filtering-connection
  // filter connections based on connection type
  locationService
    .list(ConnectionType.AkkaType)
    .map(akkaList => {
      log.info("Registered Akka connections:")
      akkaList.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
    })
  // #filtering-connection

  // #filtering-prefix
  // filter akka locations based on prefix
  locationService
    .listByPrefix("NFIRAOS.ncc")
    .map(akkaLocations => {
      log.info("Registered akka locations for nfiraos.ncc")
      akkaLocations.foreach(c => log.info(s"--- ${locationInfoToString(c)}"))
    })
  // #filtering-prefix

  if (resolveResult.isDefined) {

    // #tracking
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
    // track returns a Killswitch, that can be used to turn off notifications arbitarily
    // in this case track a connection for 5 seconds, after that schedule switching off the stream
    val killswitch = locationService
      .track(httpConnection)
      .toMat(Sink.foreach(println))(Keep.left)
      .run()
    context.system.scheduler.scheduleOnce(5.seconds) {
      killswitch.cancel()
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
    // #tracking

    // scalastyle:on print.ln

  }

  // Note: This method is never actually called in this example, since the actor does not shutdown
  // unless the application is killed. It is here to demonstrate how you might cleanly shutdown
  // tha application.
  override def postStop(): Unit = {

    // #unregister
    val unregisterF = async {
      await(httpRegResultF.map(_.unregister()))
      await(httpRegResultOnPublicNetworkF.map(_.unregister()))
      await(hcdRegResultF.map(_.unregister()))
      await(assemblyRegResultF.map(_.unregister()))
    }.flatten
    val eventualDone = async {
      await(unregisterF)
      // #unregister
      // Gracefully shutdown actor system
      await(context.system.terminate())
      // #stop-logging-system
      // Only call this once per application
      await(loggingSystem.stop)
      // #stop-logging-system
    }

    Await.result(eventualDone, 30.seconds)
  }

  @nowarn("msg=unreachable code")
  override def receive: Receive = {

    // Receive a location from the location service and if it is an akka location, send it a message
    case LocationUpdated(loc) =>
      log.info(s"Location updated ${locationInfoToString(loc)}")

    // A location was removed
    case LocationRemoved(conn) =>
      log.info(s"Location removed $conn", Map("connection" -> conn.toString))

    case AllDone =>
      log.info(s"Tracking of $exampleConnection complete.")

    case x =>
      // #log-error
      val runtimeException = new RuntimeException(s"Received unexpected message $x")
      log.error(runtimeException.getMessage, ex = runtimeException)
    // #log-error

  }
}
