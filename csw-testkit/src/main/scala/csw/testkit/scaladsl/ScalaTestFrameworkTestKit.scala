package csw.testkit.scaladsl
import akka.actor.ActorSystem
import csw.testkit._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, TestSuite}

abstract class ScalaTestFrameworkTestKit(val frameworkTestKit: FrameworkTestKit, services: Service*)
    extends TestSuite
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  /** Initialize testkit with default configuration
   *
   * By default only Location server gets started, if your tests requires other services [ex. Config, Event, Alarm etc.] along with location,
   * then use other override which accepts sequence of [[Service]] to create instance of testkit
   * */
  def this() = this(FrameworkTestKit())

  /** Initialize testkit and start all the provided services.
   *
   * @note Refer [[Service]] for supported services
   * */
  def this(services: Service*) = this(FrameworkTestKit(), services: _*)

  /** Initialize testkit with provided actorSystem */
  def this(actorSystem: ActorSystem) = this(FrameworkTestKit(actorSystem))

  /**
   * Start ConfigTestKit and LocationTestKit. If override be sure to call super.beforeAll
   * or start the testkit explicitly with `locationTestKit.startLocationServer()` and `configTestKit.startConfigServer()`.
   *
   * @note location server needs be started before starting config server as config server needs to registered with location server
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
