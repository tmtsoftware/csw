package csw.services.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.ConfigParseException
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.{AlarmMetadata, AlarmType}
import org.scalatest.{FunSuite, Matchers}

//DEOPSCSW-451: Create set of alarms based on Configuration file
class ConfigParserTest extends FunSuite with Matchers {

  test("should able to parse valid alarm metadata config file") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarm.conf")

    val expectedAlarmMetadata = AlarmMetadata(
      subsystem = "nfiraos",
      component = "cc.trombone",
      name = "tromboneAxisHighLimitAlarm",
      description = "Warns when trombone axis has reached the low limit",
      location = "south side",
      AlarmType.Absolute,
      Set(Indeterminate, Okay, Warning, Major, Critical),
      probableCause = "the trombone software has failed or the stage was driven into the low limit",
      operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
      isAutoAcknowledgeable = true,
      isLatchable = true
    )

    val actualAlarmMetadata = ConfigParser.parseAlarmMetadata(config)

    actualAlarmMetadata shouldEqual expectedAlarmMetadata
    actualAlarmMetadata.allSupportedSeverities shouldEqual Set(Indeterminate, Okay, Warning, Major, Critical)
  }

  test("should able to parse valid alarm metadata's config file") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")

    val alarmsMetadata = ConfigParser.parseAlarmsMetadata(config)
    alarmsMetadata.alarms.length shouldBe 3
  }

  test("should throw Exception while parsing invalid alarm metadata config file") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarm.conf")

    val parseException = intercept[ConfigParseException] {
      ConfigParser.parseAlarmMetadata(config)
    }
    // invalid-alarm.conf contains 3 errors:
    // 1. isLatchable missing
    // 2. invalid AlarmType
    // 3. invalid supportedSeverities
    // 3. invalid type for isAutoAcknowledgeable, expected boolean
    parseException.reasons.length shouldBe 4
  }

  test("should throw Exception while parsing invalid alarms metadata config file") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarms.conf")

    val parseException = intercept[ConfigParseException] {
      ConfigParser.parseAlarmsMetadata(config)
    }
    // invalid-alarms.conf contains 2 errors:
    // 1. subsystem missing from second alarm
    // 2. invalid AlarmType in third alarm
    parseException.reasons.length shouldBe 2
  }

}
