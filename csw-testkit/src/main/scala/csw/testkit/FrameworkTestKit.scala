package csw.testkit

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.Config
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.location.client.ActorSystemFactory
import csw.testkit.internal.TestKitUtils
import csw.testkit.scaladsl.CSWService
import csw.testkit.scaladsl.CSWService._

import scala.annotation.varargs

final class FrameworkTestKit private (
    val actorSystem: ActorSystem,
    val locationTestKit: LocationTestKit,
    val configTestKit: ConfigTestKit,
    val eventTestKit: EventTestKit,
    val alarmTestKit: AlarmTestKit
) {

  lazy val frameworkWiring: FrameworkWiring = FrameworkWiring.make(actorSystem)
  implicit val timeout: Timeout             = locationTestKit.testKitSettings.DefaultTimeout

  private var configStarted = false
  private var eventStarted  = false
  private var alarmStarted  = false

  /**
   * Before running tests, use this or [FrameworkTestKit#start] method to start required services
   *
   * This will start following services: [Location, Config, Event, Alarm]
   */
  def startAll(): Unit = start(LocationServer, ConfigServer, EventStore, AlarmStore)

  /**
   * Before running tests, use this or [FrameworkTestKit#startAll] method to start required services
   *
   * This will always start location server as it is required by all other services along with provided services
   */
  @varargs
  def start(services: CSWService*): Unit = {
    locationTestKit.startLocationServer()
    services.foreach {
      case ConfigServer   ⇒ configTestKit.startConfigServer(); configStarted = true
      case EventStore     ⇒ eventTestKit.startEventService(); eventStarted = true
      case AlarmStore     ⇒ alarmTestKit.startAlarmService(); alarmStarted = true
      case LocationServer ⇒ // location server is already started above
    }
  }

  /**
   * Use this to start component in standalone mode
   *
   * @param config configuration of standalone component
   * @return actorRef of spawned standalone component (supervisor)
   * @note before calling this, make sure you have started location server and other pre-requisite services
   *       use one of [FrameworkTestKit#startAll] or [FrameworkTestKit#start] method to start services
   */
  def spawnStandalone(config: Config): ActorRef[ComponentMessage] =
    TestKitUtils.await(Standalone.spawn(config, frameworkWiring), timeout)

  /**
   * Use this to start multiple components in container mode
   *
   * @param config configuration of container
   * @return actorRef of spawned container
   * @note before calling this, make sure you have started location server and other pre-requisite services
   *       use one of [FrameworkTestKit#startAll] or [FrameworkTestKit#start] method to start services
   */
  def spawnContainer(config: Config): ActorRef[ContainerMessage] =
    TestKitUtils.await(Container.spawn(config, frameworkWiring), timeout)

  /**
   * Shutdown all testkits which are started.
   */
  def shutdown(): Unit = {
    if (configStarted) configTestKit.shutdownConfigServer()
    if (eventStarted) eventTestKit.shutdown()
    if (alarmStarted) alarmTestKit.shutdown()
    TestKitUtils.await(Http(frameworkWiring.actorSystem).shutdownAllConnectionPools(), timeout)
    TestKitUtils.coordShutdown(frameworkWiring.actorRuntime.shutdown, timeout)
    locationTestKit.shutdownLocationServer()
  }
}

object FrameworkTestKit {

  /**
   * Scala API for creating FrameworkTestKit
   *
   * @param actorSystem actorSystem used for spawning components
   * @param locationTestKit location testkit to start location server
   * @param configTestKit config testkit to start config server
   * @param eventTestKit event testkit to start event service
   * @param alarmTestKit alarm testkit to start alarm service
   * @return handle to FrameworkTestKit which can be used to start and stop all services started
   */
  def apply(
      actorSystem: ActorSystem = ActorSystemFactory.remote("framework-testkit"),
      locationTestKit: LocationTestKit = LocationTestKit(),
      configTestKit: ConfigTestKit = ConfigTestKit(),
      eventTestKit: EventTestKit = EventTestKit(),
      alarmTestKit: AlarmTestKit = AlarmTestKit()
  ): FrameworkTestKit =
    new FrameworkTestKit(actorSystem, locationTestKit, configTestKit, eventTestKit, alarmTestKit)

  /**
   * Java API for creating FrameworkTestKit
   *
   * @return handle to FrameworkTestKit which can be used to start and stop all services started
   */
  def create(): FrameworkTestKit = apply()

  /**
   * Java API for creating FrameworkTestKit
   *
   * @param actorSystem actorSystem used for spawning components
   * @param testKitSettings custom testkit settings
   * @return handle to FrameworkTestKit which can be used to start and stop services
   */
  def create(actorSystem: ActorSystem, testKitSettings: TestKitSettings): FrameworkTestKit =
    new FrameworkTestKit(
      actorSystem,
      LocationTestKit(testKitSettings),
      ConfigTestKit(testKitSettings = testKitSettings),
      EventTestKit(testKitSettings),
      AlarmTestKit(testKitSettings)
    )
}
