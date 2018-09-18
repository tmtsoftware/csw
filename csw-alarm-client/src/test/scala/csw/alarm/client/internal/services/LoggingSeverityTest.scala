package csw.alarm.client.internal.services
import com.persist.JsonOps.{Json, JsonObject}
import com.typesafe.config.ConfigFactory
import csw.alarm.api.models.AlarmSeverity
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.logging.internal.{LoggingLevels, LoggingSystem}
import csw.logging.utils.TestAppender

import scala.collection.mutable

// DEOPSCSW-461: Log entry for severity update by component
class LoggingSeverityTest
    extends AlarmServiceTestSetup
    with SeverityServiceModule
    with MetadataServiceModule
    with StatusServiceModule {

  private val logBuffer                    = mutable.Buffer.empty[JsonObject]
  private val testAppender                 = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])
  private val loggingSystem: LoggingSystem = new LoggingSystem("logging", "version", "hostName", actorSystem)

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/more-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await

    loggingSystem.setAppenders(List(testAppender))
    loggingSystem.setDefaultLogLevel(LoggingLevels.DEBUG)
  }

  test("setCurrentSeverity should log a message") {
    val expectedMessage1 =
      "Setting severity [critical] for alarm [nfiraos-trombone-tromboneaxislowlimitalarm] with expire timeout [1] seconds"
    val expectedMessage2 = "Updating current severity [critical] in alarm store"

    setCurrentSeverity(tromboneAxisLowLimitAlarmKey, AlarmSeverity.Critical).await

    Thread.sleep(100)

    val messages = logBuffer.map(log => log("message")).toSet
    messages should contain allElementsOf Set(expectedMessage1, expectedMessage2)
  }
}
