package csw.testkit.scaladsl
import akka.actor.ActorSystem
import csw.testkit._
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
