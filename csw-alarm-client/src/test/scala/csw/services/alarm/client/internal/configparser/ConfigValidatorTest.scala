package csw.services.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.services.alarm.client.internal.configparser.SchemaRegistry._
import org.scalatest.{FunSuite, Matchers}

class ConfigValidatorTest extends FunSuite with Matchers {

  test("validation result should be successful for valid alarm metadata") {
    val config = ConfigFactory.parseResources("test-alarm.conf")
    ConfigValidator.validate(config, ALARM_SCHEMA) shouldEqual ValidationResult.Success
  }

  test("validation result should be successful for list of valid alarm's metadata") {
    val config = ConfigFactory.parseResources("test-alarms.conf")
    ConfigValidator.validate(config, ALARMS_SCHEMA) shouldEqual ValidationResult.Success
  }

}
