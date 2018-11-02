package csw.testkit.scaladsl
import com.typesafe.config.{Config, ConfigFactory}
import csw.testkit.{ConfigTestKit, LocationTestKit, TestKitSettings}

abstract class ScalaTestConfigTestKit(val configTestKit: ConfigTestKit, val locationTestKit: LocationTestKit)
    extends ScalaTestBase {

  /** Initialize testkit with default configuration */
  def this() = this(ConfigTestKit(), LocationTestKit())

  /** Initialize testkit with custom TestKitSettings */
  def this(configOpt: Option[Config] = None, testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())) =
    this(ConfigTestKit(configOpt, testKitSettings), LocationTestKit(testKitSettings))

  /**
   * Start ConfigTestKit and LocationTestKit. If override be sure to call super.beforeAll
   * or start the testkit explicitly with `locationTestKit.startLocationServer()` and `configTestKit.startConfigServer()`.
   *
   * @note location server needs be started before starting config server as config server needs to registered with location server
   */
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    locationTestKit.startLocationServer()
    configTestKit.startConfigServer()
  }

  /**
   * Shuts down the ConfigTestKit and LocationTestKit. If override be sure to call super.afterAll
   * or shut down the testkit explicitly with `configTestKit.shutdownConfigServer()` and `locationTestKit.shutdownLocationServer()`.
   */
  override protected def afterAll(): Unit = {
    super.afterAll()
    configTestKit.shutdownConfigServer()
    locationTestKit.shutdownLocationServer()
  }

}
