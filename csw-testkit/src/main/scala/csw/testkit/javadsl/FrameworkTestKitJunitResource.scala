package csw.testkit.javadsl

import java.util.Collections

import akka.actor.typed
import akka.actor.typed.{ActorRef, SpawnProtocol}
import com.typesafe.config.Config
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.location.client.extensions.LocationServiceExt
import csw.testkit._
import csw.testkit.scaladsl.CSWService
import org.junit.rules.ExternalResource

import scala.jdk.CollectionConverters._

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
 *      new FrameworkTestKitJunitResource(JCSWService.EventServer, JCSWService.ConfigServer);
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
   * then use other override which accepts sequence of [[csw.testkit.scaladsl.CSWService]] to create instance of testkit
   * */
  def this() = this(FrameworkTestKit(), Collections.emptyList())

  /** Initialize testkit and start all the provided services.
   *
   * @note Refer [[csw.testkit.scaladsl.CSWService]] for supported services
   * */
  def this(services: java.util.List[CSWService]) = this(FrameworkTestKit(), services)

  /** Initialize testkit with provided actorSystem */
  def this(actorSystem: typed.ActorSystem[SpawnProtocol.Command]) = this(FrameworkTestKit(actorSystem), Collections.emptyList())

  private val wiring = frameworkTestKit.frameworkWiring

  /** Easy access to testkits untyped actor system from java */
  lazy val actorSystem: typed.ActorSystem[SpawnProtocol.Command] = wiring.actorSystem

  /** Easy access to testkits typed actor system from java (just wraps untyped to typed). */
//  lazy val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped

  /** Handle to Java Location service  */
  lazy val jLocationService: ILocationService =
    LocationServiceExt.RichLocationService(wiring.locationService).asJava(wiring.actorRuntime.ec)

  /** Handle to Java Event service client */
  lazy val jEventService: IEventService = wiring.eventServiceFactory.jMake(jLocationService, actorSystem)

  /** Delegate to framework testkit */
  def spawnContainer(config: Config): ActorRef[ContainerMessage] = frameworkTestKit.spawnContainer(config)

  /** Delegate to framework testkit */
  def spawnStandalone(config: Config): ActorRef[ComponentMessage] = frameworkTestKit.spawnStandalone(config)

  /** Start FrameworkTestKit */
  override def before(): Unit = frameworkTestKit.start(services.asScala.toList: _*)

  /** Shuts down the FrameworkTestKit */
  override def after(): Unit = frameworkTestKit.shutdown()

}
