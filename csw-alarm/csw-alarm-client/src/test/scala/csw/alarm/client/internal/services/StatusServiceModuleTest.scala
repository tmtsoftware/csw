package csw.alarm.client.internal.services

import java.time.{Clock, LocalDateTime}

import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.api.internal.SeverityKey
import csw.alarm.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.models.AlarmSeverity._
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key.AlarmKey
import csw.alarm.models.ShelveStatus.{Shelved, Unshelved}
import csw.alarm.models.{AlarmSeverity, AlarmStatus, FullAlarmSeverity, Key}
import csw.alarm.client.internal.extensions.TimeExtensions
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.params.core.models.Subsystem
import csw.params.core.models.Subsystem.CSW
import org.scalatest.AppendedClues
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.{RedisResult, RedisValueChange}
import romaine.async.RedisAsyncApi
import romaine.reactive.RedisSubscription

class StatusServiceModuleTest
    extends AlarmServiceTestSetup
    with StatusServiceModule
    with SeverityServiceModule
    with MetadataServiceModule
    with AppendedClues {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-500: Update alarm time on current severity change
  test("reset should never update the alarm time") {
    // Initially latch and current are disconnected
    val defaultStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    defaultStatus.latchedSeverity shouldEqual Disconnected
    defaultStatus.acknowledgementStatus shouldEqual Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // Simulates initial component going to Okay and acknowledged status
    val status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay)
    status.latchedSeverity shouldBe Okay
    acknowledge(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged

    // reset the alarm and verify that time does not change
    reset(tromboneAxisLowLimitAlarmKey).await
    val statusAfterReset = getStatus(tromboneAxisLowLimitAlarmKey).await
    statusAfterReset.alarmTime.value shouldEqual status.alarmTime.value
  }

  // DEOPSCSW-447: Reset api for alarm
  test("simple test to check behavior of latch and reset") {
    getStatus(tromboneAxisLowLimitAlarmKey).await.latchedSeverity shouldEqual Disconnected
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // Move over disconnected
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay).latchedSeverity shouldEqual Okay
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    // Now make a significant alarm and check latch
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Major).latchedSeverity shouldEqual Major
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Major

    // Return to normal
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay).latchedSeverity shouldEqual Major
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    // Now reset
    reset(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.latchedSeverity shouldBe Okay
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    // Check that it's not always okay
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Critical).latchedSeverity shouldEqual Critical
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Critical
    reset(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.latchedSeverity shouldBe Critical
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Critical
  }

  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  List(Okay, Warning, Major, Indeterminate, Critical).foreach { currentSeverity =>
    test(s"reset should set latchedSeverity to current severity when current severity is $currentSeverity") {
      val defaultStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
      getMetadata(tromboneAxisLowLimitAlarmKey).await.isLatchable shouldBe true
      defaultStatus.latchedSeverity shouldEqual Disconnected
      defaultStatus.acknowledgementStatus shouldEqual Acknowledged
      getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

      // setup current severity - this does not change status
      // NOTE: This is different API than setSeverity which updates severity and status
      setCurrentSeverity(tromboneAxisLowLimitAlarmKey, currentSeverity).await
      getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe currentSeverity
      val previousStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
      previousStatus.acknowledgementStatus shouldEqual Acknowledged
      previousStatus.latchedSeverity shouldEqual Disconnected

      //reset alarm
      reset(tromboneAxisLowLimitAlarmKey).await

      val newStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

      newStatus.latchedSeverity shouldEqual currentSeverity
      // autoAck=false for tromboneAxisLowLimitAlarmKey hence ackStatus will be same as before
      newStatus.acknowledgementStatus shouldEqual Acknowledged
    }
  }

  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  test("reset should set latchedSeverity to current severity when current severity is Disconnected") {
    getMetadata(tromboneAxisLowLimitAlarmKey).await.isLatchable shouldBe true
    val defaultStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    defaultStatus.latchedSeverity shouldEqual Disconnected
    defaultStatus.acknowledgementStatus shouldEqual Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    //set current and latched severity to warning
    setSeverity(tromboneAxisLowLimitAlarmKey, Warning).await

    val originalStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

    originalStatus.latchedSeverity shouldEqual Warning
    originalStatus.acknowledgementStatus shouldEqual Unacknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Warning

    //wait for current severity to expire and get disconnected
    Thread.sleep(1500)
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // This shows that until current severity goes to a real value or reset, latchedSeverity doesn't change
    // we are relying on future Alarm Server to do this change
    val notUpdatedStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    notUpdatedStatus.latchedSeverity shouldEqual Warning
    //NOTE: this will not be the case when alarm server is running and subscribed to currentSeverity.
    //As soon as current severity changes to disconnected it, will change latched severity to disconnected
    //(unless it was already latched to critical)

    //reset alarm
    reset(tromboneAxisLowLimitAlarmKey).await

    val newStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    newStatus.acknowledgementStatus shouldEqual Acknowledged
    newStatus.latchedSeverity shouldEqual Disconnected
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(CSW, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(reset(invalidAlarm).await)
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should set acknowledgementStatus to Acknowledged of an alarm") {
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldEqual Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // set latched severity to Warning which will result status to be Latched and Unacknowledged
    val status1 = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Warning)
    status1.acknowledgementStatus shouldBe Unacknowledged

    acknowledge(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unacknowledge should set acknowledgementStatus to Unacknowledged of an alarm") {
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // set latched severity to Okay which will result status to be Latched and Acknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged

    unacknowledge(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Unacknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(CSW, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(acknowledge(invalidAlarm).await)
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unacknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(CSW, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(unacknowledge(invalidAlarm).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelve should update the status of the alarm to shelved") {
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Unshelved
    shelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Shelved

    //repeat the shelve operation
    noException shouldBe thrownBy(shelve(tromboneAxisHighLimitAlarmKey).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelve alarm should automatically get unshelved on configured time") {
    // initially make sure the shelve status of alarm is unshelved
    getStatus(tromboneAxisLowLimitAlarmKey).await.shelveStatus shouldBe Unshelved

    val currentDateTime = LocalDateTime.now(Clock.systemUTC())
    // this will create shelveTimeout in 'h:m:s AM/PM' format which will be 2 seconds ahead from current time
    val shelveTimeout = currentDateTime.plusSeconds(2).format(TimeExtensions.TimeFormatter)

    shelve(tromboneAxisLowLimitAlarmKey, shelveTimeout).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.shelveStatus shouldBe Shelved

    // after 2 seconds, expect that shelve gets timed out and inferred to unshelved
    Thread.sleep(2000)
    getStatus(tromboneAxisLowLimitAlarmKey).await.shelveStatus shouldBe Unshelved
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("unshelve should update the alarm status to unshelved") {
    // initialize alarm with Shelved status just for this test
    shelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Shelved

    unshelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Unshelved

    //repeat the unshelve operation
    noException shouldBe thrownBy(unshelve(tromboneAxisHighLimitAlarmKey).await)
  }

  test("getStatus should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(Subsystem.CSW, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(getStatus(invalidAlarm).await)
  }

  // DEOPSCSW-501: AlarmServer update time and latch severity
  test("latchToDisconnected should latch the status to disconnected") {

    //starts a "mini alarm server". This is a small alarm server impl created just for testing
    //'latchToDisconnected' api
    val redisSubscription = startAlarmWatcher()

    //awaiting for stream to become ready is very important step.
    //without this, we lose events
    redisSubscription.ready().await

    // Initially latch and current are disconnected
    val status0 = getStatus(tromboneAxisLowLimitAlarmKey).await
    status0.latchedSeverity shouldEqual Disconnected
    status0.acknowledgementStatus shouldEqual Acknowledged
    status0.initializing shouldEqual true
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // Simulates initial component going to Okay and acknowledged status
    val status1 = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay)
    status1.latchedSeverity shouldBe Okay
    status1.initializing shouldEqual false
    acknowledge(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged

    //wait for current severity to expire and get disconnected
    Thread.sleep(1500)
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    val status2: AlarmStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

    //the test stream that we have created, should have latched the alarm to Disconnected by now
    status2.latchedSeverity shouldEqual Disconnected
    status2.initializing shouldBe false
    status2.acknowledgementStatus shouldBe Unacknowledged withClue ", alarm should become Unacknowledged"
    status2.shelveStatus shouldEqual Unshelved
    status2.alarmTime.value.isAfter(status1.alarmTime.value) shouldBe true withClue ", alarm time did not change"

    //simulate component starts sending Okay heartbeat again
    val status3 = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay)

    //confirm that current severity is okay
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    //test that latched severity is still Disconnected
    status3.latchedSeverity shouldEqual Disconnected
    status3.initializing shouldBe false
    status3.acknowledgementStatus shouldBe Unacknowledged withClue ", alarm should be Unacknowledged"
    status3.shelveStatus shouldEqual Unshelved
    status3.alarmTime.value.isAfter(status1.alarmTime.value) shouldBe true withClue ", alarm time did not change"

    //shut down the alarm watcher
    redisSubscription.unsubscribe().await
  }

  /**
   * Starts a background task which continuously watches redis for all alarms severity keys.
   * Once it's detected that an alarm key has expired, this task calls 'latchToDisconnected' api
   * for that alarm key.
   *
   * In essence, this method acts like a simple version of alarm server.
   *
   * @return RedisSubscription - This subscription has a ready() and an unsubscribe() method.
   */
  private def startAlarmWatcher(): RedisSubscription = {
    //import codecs
    import csw.alarm.client.internal.AlarmRomaineCodec._

    //import helpers
    import connsFactory._

    //create async api for severity which will be needed to create keyspace api
    lazy val severityApi: RedisAsyncApi[SeverityKey, FullAlarmSeverity] = asyncApi

    //get all severity keys
    val severityKeys = metadataApi.keys(Key.GlobalKey).await.map(m => SeverityKey.fromAlarmKey(m))

    //create keyspace api for severity and return RedisSubscription
    connsFactory
      .redisKeySpaceApi(severityApi)
      .watchKeyspaceValueChange(severityKeys, overflowStrategy = OverflowStrategy.LATEST)
      .to(Sink.foreach {
        //'None' case indicates that redis key has expired
        case RedisResult(k, RedisValueChange(oldSeverityMayBe, None)) =>
          latchToDisconnected(k, oldSeverityMayBe.getOrElse(Disconnected)).await
        case _ =>
      })
      .run()
  }

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    getStatus(alarmKey).await
  }
}
