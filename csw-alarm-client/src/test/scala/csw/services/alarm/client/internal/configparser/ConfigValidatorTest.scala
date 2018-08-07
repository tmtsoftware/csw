package csw.services.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.internal.ValidationResult
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-451: Create set of alarms based on Configuration file
class ConfigValidatorTest extends FunSuite with Matchers {
  test("validation result should be successful for list of valid alarm's metadata") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    ConfigValidator.validate(config, ConfigParser.ALARMS_SCHEMA) shouldEqual ValidationResult.Success
  }

  test("validation result should be failed for invalid alarms metadata") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarms.conf")
    val result = ConfigValidator.validate(config, ConfigParser.ALARMS_SCHEMA)
    result shouldBe a[ValidationResult.Failure]
  }
}
