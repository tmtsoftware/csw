package csw.testkit.scaladsl

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import com.typesafe.config.Config
import csw.command.client.messages.{ComponentMessage, ContainerMessage, TopLevelActorMessage}
import csw.command.client.models.framework.LocationServiceUsage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.Connection
import csw.prefix.models.Prefix
import csw.testkit.*

import scala.concurrent.duration.FiniteDuration

/**
 * A ScalaTest base class for the [[FrameworkTestKit]], making it possible to have ScalaTest manage the lifecycle of the testkit.
 *
 * The testkit will be automatically start list of provided [[CSWService]]
 * and shut down all started [[CSWService]] when the test completes or fails using ScalaTest's BeforeAndAfterAll trait.
 *
 * If a spec overrides beforeAll or afterAll, it must call super.beforeAll and super.afterAll respectively.
 */
abstract class ScalaTestFrameworkTestKit(val frameworkTestKit: FrameworkTestKit, services: CSWService*) extends ScalaTestBase {

  /**
   * Initialize testkit with default configuration
   *
   * By default only Location server gets started, if your tests requires other services [ex. Config, Event, Alarm etc.] along with location,
   * then use other override which accepts sequence of [[CSWService]] to create instance of testkit
   */
  def this() = this(FrameworkTestKit())

  /**
   * Initialize testkit and start all the provided services.
   *
   * @note Refer [[CSWService]] for supported services
   */
  def this(services: CSWService*) = this(FrameworkTestKit(), services*)

  /** Initialize testkit with provided actorSystem */
  def this(actorSystem: ActorSystem[SpawnProtocol.Command]) = this(FrameworkTestKit(actorSystem))

  /** Delegate to framework testkit */
  def spawnContainer(config: Config): ActorRef[ContainerMessage] = frameworkTestKit.spawnContainer(config)

  /** Delegate to framework testkit */
  def spawnStandalone(config: Config): ActorRef[ComponentMessage] = frameworkTestKit.spawnStandalone(config)

  /** Delegate to framework testkit */
  def spawnHCD(
      prefix: Prefix,
      behaviorFactory: (ActorContext[TopLevelActorMessage], CswContext) => ComponentHandlers,
      locationServiceUsage: LocationServiceUsage = LocationServiceUsage.RegisterOnly,
      connections: Set[Connection] = Set.empty,
      initializeTimeout: FiniteDuration = frameworkTestKit.timeout.duration
  ): ActorRef[ComponentMessage] =
    frameworkTestKit.spawnHCD(prefix, behaviorFactory, locationServiceUsage, connections, initializeTimeout)

  /** Delegate to framework testkit */
  def spawnAssembly(
      prefix: Prefix,
      behaviorFactory: (ActorContext[TopLevelActorMessage], CswContext) => ComponentHandlers,
      locationServiceUsage: LocationServiceUsage = LocationServiceUsage.RegisterOnly,
      connections: Set[Connection] = Set.empty,
      initializeTimeout: FiniteDuration = frameworkTestKit.timeout.duration
  ): ActorRef[ComponentMessage] =
    frameworkTestKit.spawnAssembly(prefix, behaviorFactory, locationServiceUsage, connections, initializeTimeout)

  /**
   * Start FrameworkTestKit. If override be sure to call super.beforeAll
   * or start the testkit explicitly with `frameworkTestKit.start()`.
   */
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.start(services*)
  }

  /**
   * Shuts down the FrameworkTestKit. If override be sure to call super.afterAll
   * or shut down the testkit explicitly with `frameworkTestKit.shutdown()`.
   */
  override protected def afterAll(): Unit = {
    super.afterAll()
    frameworkTestKit.shutdown()
  }
}
