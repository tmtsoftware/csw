package csw.testkit.scaladsl
import csw.testkit.{LocationTestKit, TestKitSettings}

/**
 * A ScalaTest base class for the [[LocationTestKit]], making it possible to have ScalaTest manage the lifecycle of the testkit.
 *
 * The testkit will be automatically start LocationServer
 * and shut down it when the test completes or fails using ScalaTest's BeforeAndAfterAll trait.
 *
 * If a spec overrides beforeAll or afterAll, it must call super.beforeAll and super.afterAll respectively.
 */
abstract class ScalaTestLocationTestKit(testKit: LocationTestKit) extends ScalaTestBase {

  /** Initialize testkit with default configuration */
  def this() = this(LocationTestKit())

  /** Initialize testkit with custom TestKitSettings */
  def this(testKitSettings: TestKitSettings) = this(LocationTestKit(testKitSettings))

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
