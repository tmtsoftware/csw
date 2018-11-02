package csw.testkit.javadsl

import java.util.Optional

import csw.testkit.{LocationTestKit, TestKitSettings}
import org.junit.rules.ExternalResource

import scala.compat.java8.OptionConverters.RichOptionalGeneric

/**
 * A Junit external resource for the [[LocationTestKit]], making it possible to have Junit manage the lifecycle of the testkit.
 * The testkit will be automatically shut down when the test completes or fails.
 */
final class LocationTestKitJunitResource(val locationTestKit: LocationTestKit) extends ExternalResource {

  /** Initialize testkit with default configuration */
  def this() = this(LocationTestKit())

  /** Initialize testkit with custom TestKitSettings */
  def this(testKitSettings: TestKitSettings) = this(LocationTestKit(testKitSettings))

  /**
   * Initialize testkit with custom configuration
   *
   * @param clusterPort port on which location server akka cluster runs
   * @param testKitSettings custom TestKitSettings
   * @return LocationTestKitJunitResource which can be mixed in with tests
   */
  def this(clusterPort: Optional[Int], testKitSettings: Option[TestKitSettings]) =
    this(LocationTestKit(clusterPort.asScala, testKitSettings))

  /** Start LocationTestKit */
  override def before(): Unit = locationTestKit.startLocationServer()

  /** Shuts down the LocationTestKit */
  override def after(): Unit = locationTestKit.shutdownLocationServer()

}
