package csw.testkit.scaladsl

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.testkit._

/**
 * A ScalaTest base class for the [[FrameworkTestKit]], making it possible to have ScalaTest manage the lifecycle of the testkit.
 *
 * The testkit will be automatically start list of provided [[CSWService]]
 * and shut down all started [[CSWService]] when the test completes or fails using ScalaTest's BeforeAndAfterAll trait.
 *
 * If a spec overrides beforeAll or afterAll, it must call super.beforeAll and super.afterAll respectively.
 */
abstract class ScalaTestFrameworkTestKit(val frameworkTestKit: FrameworkTestKit, services: CSWService*) extends ScalaTestBase {

  /** Initialize testkit with default configuration
   *
   * By default only Location server gets started, if your tests requires other services [ex. Config, Event, Alarm etc.] along with location,
   * then use other override which accepts sequence of [[CSWService]] to create instance of testkit
   * */
  def this() = this(FrameworkTestKit())

  /** Initialize testkit and start all the provided services.
   *
   * @note Refer [[CSWService]] for supported services
   * */
  def this(services: CSWService*) = this(FrameworkTestKit(), services: _*)

  /** Initialize testkit with provided actorSystem */
  def this(actorSystem: ActorSystem) = this(FrameworkTestKit(actorSystem))

  /** Delegate to framework testkit */
  def spawnContainer(config: Config): ActorRef[ContainerMessage] = frameworkTestKit.spawnContainer(config)

  /** Delegate to framework testkit */
  def spawnStandalone(config: Config): ActorRef[ComponentMessage] = frameworkTestKit.spawnStandalone(config)

  /**
   * Start FrameworkTestKit. If override be sure to call super.beforeAll
   * or start the testkit explicitly with `frameworkTestKit.start()`.
   */
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.start(services: _*)
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
