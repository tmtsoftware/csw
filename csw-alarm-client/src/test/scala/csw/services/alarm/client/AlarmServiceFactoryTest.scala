package csw.services.alarm.client
import com.typesafe.config.ConfigFactory
import csw.commons.utils.SocketUtils.getFreePort
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.alarm.api.models.AlarmSeverity.Indeterminate
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
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
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    alarmService.initAlarms(validAlarmsConfig, reset = true).await
  }

  override protected def afterAll(): Unit = {
    locationService.shutdown(TestFinishedReason).await
    super.afterAll()
  }

  test("should create admin alarm service using location service") {
    val alarmServiceUsingLS: AlarmAdminService = alarmServiceFactory.makeAdminApi(locationService)
    alarmServiceUsingLS.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldEqual tromboneAxisHighLimitAlarm
  }

  test("should create admin alarm service using host and port") {
    val alarmServiceUsingHostAndPort: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort)
    alarmServiceUsingHostAndPort.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldEqual tromboneAxisHighLimitAlarm
  }

  test("should create client alarm service using location service") {
    val alarmServiceUsingLS: AlarmService = alarmServiceFactory.makeClientApi(locationService)
    alarmServiceUsingLS.setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldEqual Indeterminate
  }

  test("should create client alarm service using host and port") {
    val alarmServiceUsingUsingHostAndPort: AlarmService = alarmServiceFactory.makeClientApi(hostname, sentinelPort)
    alarmServiceUsingUsingHostAndPort.setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldEqual Indeterminate
  }

}
