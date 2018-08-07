package csw.services.alarm.client
import java.io.File

import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.alarm.api.models.AlarmSeverity.Indeterminate
import csw.services.alarm.client.internal.commons.AlarmServiceConnection
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.commons.LogAdminActorFactory

// DEOPSCSW-481: Component Developer API available to all CSW components
class AlarmServiceFactoryTest extends AlarmServiceTestSetup {

  private val seedSystem      = ClusterAwareSettings.onPort(getFreePort).system
  private val locationService = LocationServiceFactory.withSystem(seedSystem)

  locationService
    .register(TcpRegistration(AlarmServiceConnection.value, sentinelPort, LogAdminActorFactory.make(seedSystem)))
    .await

  override protected def beforeEach(): Unit = {
    val validAlarmsFile   = new File(getClass.getResource("/test-alarms/valid-alarms.conf").getPath)
    val validAlarmsConfig = ConfigFactory.parseFile(validAlarmsFile).resolve(ConfigResolveOptions.noSystem())
    alarmService.initAlarms(validAlarmsConfig, reset = true).await
  }

  override protected def afterAll(): Unit = {
    locationService.shutdown(TestFinishedReason).await
    super.afterAll()
  }

  test("should create admin alarm service using location service") {
    val alarmServiceUsingLS = alarmServiceFactory.adminApi(locationService).await
    alarmServiceUsingLS.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldEqual tromboneAxisHighLimitAlarm
  }

  test("should create admin alarm service using host and port") {
    val alarmServiceUsingLS = alarmServiceFactory.adminApi(hostname, sentinelPort).await
    alarmServiceUsingLS.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldEqual tromboneAxisHighLimitAlarm
  }

  test("should create client alarm service using location service") {
    val alarmServiceUsingLS = alarmServiceFactory.clientApi(locationService).await
    alarmServiceUsingLS.setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldEqual Indeterminate
  }

  test("should create client alarm service using host and port") {
    val alarmServiceUsingLS = alarmServiceFactory.adminApi(hostname, sentinelPort).await
    alarmServiceUsingLS.setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldEqual Indeterminate
  }

}
