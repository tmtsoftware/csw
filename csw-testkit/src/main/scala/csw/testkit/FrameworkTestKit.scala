package csw.testkit

import akka.actor.typed
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.command.client.models.framework.LocationServiceUsage
import csw.config.api.scaladsl.ConfigClientService
import csw.event.api.scaladsl.EventService
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.location.api.models.{ComponentType, Connection}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import csw.testkit.internal.{SpawnComponent, TestKitUtils}
import csw.testkit.scaladsl.CSWService
import csw.testkit.scaladsl.CSWService._
import io.lettuce.core.RedisClient

import scala.annotation.varargs
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * FrameworkTestKit supports starting one or more services from [[csw.testkit.scaladsl.CSWService]]
 * It also provides helpers to spawn components in standalone or container mode
 *
 * Example:
 * {{{
 *   private val testKit = FrameworkTestKit()
 *
 *   // this will start Event and Alarm Server (Note: Location Server will always be started as it is required by all other services)
 *   testKit.start(EventServer, AlarmServer)
 *
 *   // spawn component in standalone mode
 *   testKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
 *
 *   // stopping services
 *   testKit.shutdown()
 *
 * }}}
 */
final class FrameworkTestKit private (
    system: ActorSystem[SpawnProtocol.Command],
    val redisClient: RedisClient,
    val locationTestKit: LocationTestKit,
    val configTestKit: ConfigTestKit,
    val eventTestKit: EventTestKit,
    val alarmTestKit: AlarmTestKit
) {
  private lazy val frameworkWiring              = FrameworkWiring.make(system, redisClient)
  private[testkit] lazy val eventServiceFactory = frameworkWiring.eventServiceFactory

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = system
  implicit lazy val ec: ExecutionContext                            = system.executionContext
  lazy val locationService: LocationService                         = frameworkWiring.locationService
  lazy val configClientService: ConfigClientService                 = frameworkWiring.configClientService
  lazy val eventService: EventService                               = eventServiceFactory.make(locationService)
  lazy val alarmServiceFactory: AlarmServiceFactory                 = frameworkWiring.alarmServiceFactory

  lazy val timeout: Timeout = locationTestKit.timeout

  private var locationTestkitWithAuth: LocationTestKit = _

  private var configStarted           = false
  private var eventStarted            = false
  private var alarmStarted            = false
  private var locationWithAuthStarted = false

  /**
   * Before running tests, use this or [FrameworkTestKit#start] method to start required services
   *
   * This will start following services: [LocationServer, ConfigServer, EventServer, AlarmServer]
   */
  def startAll(): Unit = start(LocationServer, ConfigServer, EventServer, AlarmServer)

  /**
   * Before running tests, use this or [FrameworkTestKit#startAll] method to start required services
   *
   * This will always start location server as it is required by all other services along with provided services
   */
  @varargs
  def start(services: CSWService*): Unit = {
    locationTestKit.startLocationServer()
    services.foreach {
      case ConfigServer   => configTestKit.startConfigServer(); configStarted = true
      case EventServer    => eventTestKit.startEventService(); eventStarted = true
      case AlarmServer    => alarmTestKit.startAlarmService(); alarmStarted = true
      case LocationServer => // location server without auth is already started above
      case LocationServerWithAuth =>
        locationTestkitWithAuth = LocationTestKit.withAuth(system.settings.config)
        locationTestkitWithAuth.startLocationServer()
        locationWithAuthStarted = true
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
   * Use this to start hcd in standalone mode
   *
   * @param prefix prefix of the hcd
   * @param behaviorFactory behavior factory for the hcd
   * @param locationServiceUsage location service usages of the hcd
   * @param connections connections to track for the hcd
   * @param initializeTimeout initialize timeout for the hcd
   * @return actorRef of spawned hcd
   * @note before calling this, make sure you have started location server and other pre-requisite services
   *       use one of [FrameworkTestKit#startAll] or [FrameworkTestKit#start] method to start services
   */
  def spawnHCD(
      prefix: Prefix,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage = LocationServiceUsage.RegisterOnly,
      connections: Set[Connection] = Set.empty,
      initializeTimeout: FiniteDuration = 10.seconds
  ): ActorRef[ComponentMessage] =
    TestKitUtils.await(
      SpawnComponent.spawnComponent(
        frameworkWiring,
        prefix,
        ComponentType.HCD,
        behaviorFactory,
        locationServiceUsage,
        connections,
        initializeTimeout
      ),
      timeout
    )

  /**
   * Use this to start assembly in standalone mode
   *
   * @param prefix prefix of the assembly
   * @param behaviorFactory behavior factory for the assembly
   * @param locationServiceUsage location service usages of the assembly
   * @param connections connections to track for the assembly
   * @param initializeTimeout initialize timeout for the assembly
   * @return actorRef of spawned assembly
   * @note before calling this, make sure you have started location server and other pre-requisite services
   *       use one of [FrameworkTestKit#startAll] or [FrameworkTestKit#start] method to start services
   */
  def spawnAssembly(
      prefix: Prefix,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage = LocationServiceUsage.RegisterOnly,
      connections: Set[Connection] = Set.empty,
      initializeTimeout: FiniteDuration = 10.seconds
  ): ActorRef[ComponentMessage] =
    TestKitUtils.await(
      SpawnComponent.spawnComponent(
        frameworkWiring,
        prefix,
        ComponentType.Assembly,
        behaviorFactory,
        locationServiceUsage,
        connections,
        initializeTimeout
      ),
      timeout
    )

  /**
   * Shutdown all testkits which are started.
   */
  def shutdown(): Unit = {
    redisClient.shutdown()
    if (configStarted) {
      configTestKit.deleteServerFiles()
      configTestKit.terminateServer()
    }
    if (eventStarted) eventTestKit.stopRedis()
    if (alarmStarted) alarmTestKit.stopRedis()
    if (locationWithAuthStarted) locationTestkitWithAuth.shutdownLocationServer()
    TestKitUtils.shutdown(frameworkWiring.actorRuntime.shutdown(), timeout)
    locationTestKit.shutdownLocationServer()
  }
}

object FrameworkTestKit {

  /**
   * Scala API for creating FrameworkTestKit
   *
   * @param actorSystem actorSystem used for spawning components
   * @param testKitSettings custom testKitSettings
   * @return handle to FrameworkTestKit which can be used to start and stop all services started
   */
  def apply(
      actorSystem: typed.ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "framework-testkit"),
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): FrameworkTestKit = {
    val redisClient = RedisClient.create()
    new FrameworkTestKit(
      actorSystem,
      redisClient,
      LocationTestKit(testKitSettings),
      ConfigTestKit(actorSystem, testKitSettings = testKitSettings),
      EventTestKit(actorSystem, testKitSettings),
      AlarmTestKit(actorSystem, redisClient, testKitSettings)
    )
  }

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
   * @return handle to FrameworkTestKit which can be used to start and stop all services started
   */
  def create(actorSystem: typed.ActorSystem[SpawnProtocol.Command]): FrameworkTestKit = apply(actorSystem)

  /**
   * Java API for creating FrameworkTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to FrameworkTestKit which can be used to start and stop all services started
   */
  def create(testKitSettings: TestKitSettings): FrameworkTestKit = apply(testKitSettings = testKitSettings)

}
