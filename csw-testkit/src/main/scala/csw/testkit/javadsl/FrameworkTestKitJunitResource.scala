package csw.testkit.javadsl

import java.util.Collections

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.testkit._
import csw.testkit.scaladsl.CSWService
import org.junit.rules.ExternalResource

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

/**
 * A Junit external resource for the [[FrameworkTestKit]], making it possible to have Junit manage the lifecycle of the testkit.
 * The testkit will be automatically shut down when the test completes or fails.
 *
 * Example:
 * {{{
 * public class JFrameworkExampleTest {
 *
 *  @ClassRule
 *   public static final FrameworkTestKitJunitResource testKit =
 *      new FrameworkTestKitJunitResource(JCSWService.EventStore, JCSWService.ConfigServer);
 *
 *   @Test
 *   public void spawnContainer() {
 *      ActorRef<ContainerMessage> containerRef =
 *      testKit.spawnContainer(ConfigFactory.load("container.conf"))
 *   }
 *
 * }
 * }}}
 *
 */
final class FrameworkTestKitJunitResource(val frameworkTestKit: FrameworkTestKit, services: java.util.List[CSWService])
    extends ExternalResource {

  /** Initialize testkit with default configuration
   *
   * By default only Location server gets started, if your tests requires other services [ex. Config, Event, Alarm etc.] along with location,
   * then use other override which accepts sequence of [[CSWService]] to create instance of testkit
   * */
  def this() = this(FrameworkTestKit(), Collections.emptyList())

  /** Initialize testkit and start all the provided services.
   *
   * @note Refer [[CSWService]] for supported services
   * */
  def this(services: java.util.List[CSWService]) = this(FrameworkTestKit(), services)

  /** Initialize testkit with provided actorSystem */
  def this(actorSystem: ActorSystem) = this(FrameworkTestKit(actorSystem), Collections.emptyList())

  def spawnContainer(config: Config): ActorRef[ContainerMessage] = frameworkTestKit.spawnContainer(config)

  def spawnStandalone(config: Config): ActorRef[ComponentMessage] = frameworkTestKit.spawnStandalone(config)

  /**
   * Start FrameworkTestKit.
   */
  override def before(): Unit = frameworkTestKit.start(services.asScala.toList: _*)

  /**
   * Shuts down the FrameworkTestKit.
   */
  override def after(): Unit = frameworkTestKit.shutdown()

}
