package csw.services.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.{AlarmMetadata, AlarmType}
import org.scalatest.{FunSuite, Matchers}

class ConfigParserTest extends FunSuite with Matchers {

  test("should able to parse valid alarm metadata config file") {
    val config = ConfigFactory.parseResources("test-alarm.conf")

    ConfigParser.parseAlarmMetadata(config) shouldEqual AlarmMetadata(
      subsystem = "nfiraos",
      component = "nfiraos.cc.trombone",
      name = "tromboneAxisHighLimitAlarm",
      description = "Warns when trombone axis has reached the low limit",
      location = "south side",
      AlarmType.Absolute,
      List(Indeterminate, Okay, Warning, Major, Critical),
      probableCause = "the trombone software has failed or the stage was driven into the low limit",
      operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
      isAutoAcknowledgeable = true,
      isLatchable = true
    )
  }

  test("should able to parse valid alarm metadata's config file") {
    val config = ConfigFactory.parseResources("test-alarms.conf")

    val alarmsMetadata = ConfigParser.parseAlarmsMetadata(config)
    alarmsMetadata.alarms.length shouldBe 3
  }

}
