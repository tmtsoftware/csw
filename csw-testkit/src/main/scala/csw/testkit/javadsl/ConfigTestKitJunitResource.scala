package csw.testkit.javadsl

import java.util.Optional

import csw.testkit.{ConfigTestKit, LocationTestKit, TestKitSettings}
import org.junit.rules.ExternalResource

import scala.compat.java8.OptionConverters.RichOptionalGeneric

/**
 * A Junit external resource for the [[ConfigTestKit]], making it possible to have Junit manage the lifecycle of the testkit.
 * The testkit will be automatically shut down when the test completes or fails.
 *
 * Example:
 * {{{
 * public class JConfigClientExampleTest {
 *
 *  @ClassRule
 *   public static final ConfigTestKitJunitResource testKit = new ConfigTestKitJunitResource();
 *
 *   @After
 *   public void deleteServerFiles() {
 *       testKit.configTestKit().deleteServerFiles();
 *   }
 *
 * }
 * }}}
 */
final class ConfigTestKitJunitResource(val configTestKit: ConfigTestKit, val locationTestKit: LocationTestKit)
    extends ExternalResource {

  /** Initialize testkit with default configuration */
  def this() = this(ConfigTestKit(), LocationTestKit())

  /** Initialize testkit with custom TestKitSettings */
  def this(testKitSettings: TestKitSettings) = this(ConfigTestKit(testKitSettings), LocationTestKit(testKitSettings))

  /**
   * Initialize testkit with custom configuration
   *
   * @param configPort port on which config server to run
   * @param locationPort port on which location server akka cluster runs
   * @param testKitSettings custom TestKitSettings
   * @return ConfigTestKitJunitResource which can be mixed in with tests
   */
  def this(configPort: Int, locationPort: Optional[Int], testKitSettings: Optional[TestKitSettings]) =
    this(ConfigTestKit(configPort, testKitSettings.asScala), LocationTestKit(locationPort.asScala, testKitSettings.asScala))

  /**
   * Start ConfigTestKit and LocationTestKit.
   *
   * @note location server needs be started before starting config server as config server needs to registered with location server
   */
  override def before(): Unit = {
    locationTestKit.startLocationServer()
    configTestKit.startConfigServer()
  }

  /**
   * Shuts down the ConfigTestKit and LocationTestKit.
   */
  override def after(): Unit = {
    configTestKit.shutdownConfigServer()
    locationTestKit.shutdownLocationServer()
  }

}
