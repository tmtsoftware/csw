package csw.services.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{LGSF, NFIRAOS, TCS}
import csw.services.alarm.api.exceptions.ConfigParseException
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.AlarmType.Absolute
import csw.services.alarm.api.models.{AlarmMetadata, AlarmType}
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-451: Create set of alarms based on Configuration file
// DEOPSCSW-452: Represent alarm in a model as defined in Configuration file
class ConfigParserTest extends FunSuite with Matchers {

  test("should able to parse valid alarm metadata's config file") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")

    val expectedAlarmMetadataSet = Set(
      AlarmMetadata(
        subsystem = NFIRAOS,
        component = "trombone",
        name = "tromboneAxisHighLimitAlarm",
        description = "Warns when trombone axis has reached the high limit",
        location = "south side",
        AlarmType.Absolute,
        Set(Indeterminate, Okay, Warning, Major),
        probableCause = "the trombone software has failed or the stage was driven into the high limit",
        operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
        isAutoAcknowledgeable = true,
        isLatchable = true,
        activationStatus = Active
      ),
      AlarmMetadata(
        subsystem = NFIRAOS,
        component = "trombone",
        name = "tromboneAxisLowLimitAlarm",
        description = "Warns when trombone axis has reached the low limit",
        location = "south side",
        alarmType = AlarmType.Absolute,
        Set(Warning, Major, Critical, Indeterminate, Okay),
        probableCause = "the trombone software has failed or the stage was driven into the low limit",
        operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
        isAutoAcknowledgeable = false,
        isLatchable = true,
        activationStatus = Active
      ),
      AlarmMetadata(
        subsystem = TCS,
        component = "tcsPk",
        name = "cpuExceededAlarm",
        description =
          "This alarm is activated when the tcsPk Assembly can no longer calculate all of its pointing values in the time allocated. The CPU may lock power, or there may be pointing loops running that are not needed. Response: Check to see if pointing loops are executing that are not needed or see about a more powerful CPU.",
        location = "in computer...",
        alarmType = Absolute,
        supportedSeverities = Set(Indeterminate, Okay, Warning, Major, Critical),
        probableCause = "too fast...",
        operatorResponse = "slow it down...",
        isAutoAcknowledgeable = true,
        isLatchable = false,
        activationStatus = Active
      ),
      AlarmMetadata(
        subsystem = LGSF,
        component = "tcsPkInactive",
        name = "cpuIdleAlarm",
        description = "This alarm is activated CPU is idle",
        location = "in computer...",
        alarmType = Absolute,
        supportedSeverities = Set(Indeterminate, Okay, Warning, Major, Critical),
        probableCause = "too fast...",
        operatorResponse = "slow it down...",
        isAutoAcknowledgeable = true,
        isLatchable = false,
        activationStatus = Inactive
      )
    )

    val actualAlarmMetadataSet = ConfigParser.parseAlarmMetadataSet(config)

    actualAlarmMetadataSet.alarms shouldEqual expectedAlarmMetadataSet
  }

  test("should throw Exception while parsing invalid alarms metadata config file") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarms.conf")

    val parseException = the[ConfigParseException] thrownBy ConfigParser.parseAlarmMetadataSet(config)

    parseException.reasons.map(_.split("invalid-alarms.conf, ").tail.head).toSet shouldEqual Set(
      "at path: /alarms/0/subsystem: error: instance value (\"INVALID\") not found in enum (possible values: [\"AOESW\",\"APS\",\"CIS\",\"CSW\",\"DMS\",\"DPS\",\"ENC\",\"ESEN\",\"ESW\",\"GMS\",\"IRIS\",\"IRMS\",\"LGSF\",\"M1CS\",\"M2CS\",\"M3CS\",\"MCS\",\"NFIRAOS\",\"NSCU\",\"OSS\",\"PFCS\",\"PSFR\",\"RTC\",\"RPG\",\"SCMS\",\"SOSS\",\"STR\",\"SUM\",\"TCS\",\"TINC\",\"WFOS\",\"Container\"]) (schema: config:/alarm-schema.conf#:/properties/subsystem)",
      "at path: /alarms/1: error: object has missing required properties ([\"subsystem\"]) (schema: config:/alarm-schema.conf#:)",
      "at path: /alarms/2/alarmType: error: instance value (\"Invalid\") not found in enum (possible values: [\"Absolute\",\"BitPattern\",\"Calculated\",\"Deviation\",\"Discrepancy\",\"Instrument\",\"RateChange\",\"RecipeDriven\",\"Safety\",\"Statistical\",\"System\"]) (schema: config:/alarm-schema.conf#:/properties/alarmType)"
    )
  }

}
