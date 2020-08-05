package csw.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.alarm.models.AlarmSeverity
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.utils.TestAppender
import csw.logging.models.Level.DEBUG
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable

// DEOPSCSW-461: Log entry for severity update by component
class LoggingSeverityTest
    extends AlarmServiceTestSetup
    with SeverityServiceModule
    with MetadataServiceModule
    with StatusServiceModule
    with Eventually {

  private val logBuffer                    = mutable.Buffer.empty[JsObject]
  private val testAppender                 = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])
  private val loggingSystem: LoggingSystem = new LoggingSystem("logging", "version", "hostName", actorSystem)

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/more-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await

    loggingSystem.setAppenders(List(testAppender))
    loggingSystem.setDefaultLogLevel(DEBUG)
  }

  test("setCurrentSeverity should log a message | DEOPSCSW-461") {
    val expectedMessage1 =
      "Setting severity [Critical] for alarm [nfiraos-trombone-tromboneaxislowlimitalarm] with expire timeout [1] seconds"
    val expectedMessage2 = "Updating current severity [Critical] in alarm store"

    setCurrentSeverity(tromboneAxisLowLimitAlarmKey, AlarmSeverity.Critical).await

    eventually {
      val messages = logBuffer.map(_.getString("message")).toSet
      messages should contain allElementsOf Set(expectedMessage1, expectedMessage2)
    }
  }
}
