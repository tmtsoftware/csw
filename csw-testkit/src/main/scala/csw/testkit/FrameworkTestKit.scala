package csw.testkit

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.location.client.ActorSystemFactory
import csw.testkit.internal.TestKitUtils
import csw.testkit.scaladsl.CSWService
import csw.testkit.scaladsl.CSWService._

import scala.annotation.varargs
import scala.concurrent.ExecutionContext

/**
 * FrameworkTestKit supports starting one or more services from [[CSWService]]
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
 *
 */
final class FrameworkTestKit private (
    val actorSystem: ActorSystem,
    val locationTestKit: LocationTestKit,
    val configTestKit: ConfigTestKit,
    val eventTestKit: EventTestKit,
    val alarmTestKit: AlarmTestKit
) {

  implicit lazy val system: ActorSystem     = actorSystem
  lazy val frameworkWiring: FrameworkWiring = FrameworkWiring.make(actorSystem)
  implicit lazy val ec: ExecutionContext    = frameworkWiring.actorRuntime.ec
  implicit lazy val mat: Materializer       = frameworkWiring.actorRuntime.mat

  implicit val timeout: Timeout = locationTestKit.timeout

  private var configStarted = false
  private var eventStarted  = false
  private var alarmStarted  = false

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
      case ConfigServer   ⇒ configTestKit.startConfigServer(); configStarted = true
      case EventServer    ⇒ eventTestKit.startEventService(); eventStarted = true
      case AlarmServer    ⇒ alarmTestKit.startAlarmService(); alarmStarted = true
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
    TestKitUtils.await(Http().shutdownAllConnectionPools(), timeout)
    if (configStarted) configTestKit.deleteServerFiles(); configTestKit.terminateServer()
    if (eventStarted) eventTestKit.stopRedis()
    if (alarmStarted) alarmTestKit.stopRedis()
    TestKitUtils.coordShutdown(frameworkWiring.actorRuntime.shutdown, timeout)
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
      actorSystem: ActorSystem = ActorSystemFactory.remote("framework-testkit"),
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): FrameworkTestKit = new FrameworkTestKit(
    actorSystem,
    LocationTestKit(testKitSettings),
    ConfigTestKit(actorSystem, testKitSettings = testKitSettings),
    EventTestKit(actorSystem, testKitSettings),
    AlarmTestKit(actorSystem, testKitSettings)
  )

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
  def create(actorSystem: ActorSystem): FrameworkTestKit = apply(actorSystem = actorSystem)

  /**
   * Java API for creating FrameworkTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to FrameworkTestKit which can be used to start and stop all services started
   */
  def create(testKitSettings: TestKitSettings): FrameworkTestKit = apply(testKitSettings = testKitSettings)

}
