package csw.testkit.scaladsl
import csw.testkit.{LocationTestKit, TestKitSettings}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

abstract class ScalaTestLocationTestKit(testKit: LocationTestKit)
    extends TestSuite
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  /** Initialize testkit with default configuration */
  def this() = this(LocationTestKit())

  /** Initialize testkit with custom TestKitSettings */
  def this(testKitSettings: TestKitSettings) = this(LocationTestKit(testKitSettings))

  /**
   * Initialize testkit with custom configuration
   *
   * @param clusterPort port on which location server akka cluster runs
   * @param testKitSettings custom TestKitSettings
   * @return ScalaTestLocationTestKit which can be mixed in with tests
   */
  def this(clusterPort: Option[Int], testKitSettings: Option[TestKitSettings]) =
    this(LocationTestKit(clusterPort, testKitSettings))

  /**
   * Start LocationTestKit. If override be sure to call super.beforeAll
   * or start the testkit explicitly with `locationTestKit.startLocationServer()`.
   */
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    testKit.startLocationServer()
  }

  /**
   * Shuts down the LocationTestKit. If override be sure to call super.afterAll
   * or shut down the testkit explicitly with `testKit.shutdownLocationServer()`.
   */
  override protected def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownLocationServer()
  }

}
