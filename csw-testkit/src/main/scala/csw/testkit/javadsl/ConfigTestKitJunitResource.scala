package csw.testkit.javadsl

import com.typesafe.config.Config
import csw.testkit.{ConfigTestKit, LocationTestKit, TestKitSettings}
import org.junit.rules.ExternalResource

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
  def this(testKitSettings: TestKitSettings) =
    this(ConfigTestKit(testKitSettings = testKitSettings), LocationTestKit(testKitSettings))

  /** Initialize testkit with custom Configuration */
  def this(config: Config) = this(ConfigTestKit(Some(config)), LocationTestKit())

  /**
   * Initialize testkit with custom configuration
   *
   * @param config custom configuration with which to start config server
   * @param testKitSettings custom TestKitSettings
   * @return ConfigTestKitJunitResource which can be mixed in with tests
   */
  def this(config: Config, testKitSettings: TestKitSettings) =
    this(ConfigTestKit(Some(config), testKitSettings), LocationTestKit(testKitSettings))

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
