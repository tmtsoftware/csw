package csw.testkit.scaladsl
import csw.testkit.{LocationTestKit, TestKitSettings}

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
