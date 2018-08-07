package csw.services.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.internal.ValidationResult
import csw.services.alarm.client.internal.configparser.SchemaRegistry._
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-451: Create set of alarms based on Configuration file
class ConfigValidatorTest extends FunSuite with Matchers {

  test("validation result should be successful for valid alarm metadata") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarm.conf")
    ConfigValidator.validate(config, ALARM_SCHEMA) shouldEqual ValidationResult.Success
  }

  test("validation result should be successful for list of valid alarm's metadata") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    ConfigValidator.validate(config, ALARMS_SCHEMA) shouldEqual ValidationResult.Success
  }

  test("validation result should be failed for invalid alarm metadata") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarm.conf")
    val result = ConfigValidator.validate(config, ALARM_SCHEMA)
    result shouldBe a[ValidationResult.Failure]
  }

  test("validation result should be failed for invalid alarms metadata") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarms.conf")
    val result = ConfigValidator.validate(config, ALARMS_SCHEMA)
    result shouldBe a[ValidationResult.Failure]
  }

}
