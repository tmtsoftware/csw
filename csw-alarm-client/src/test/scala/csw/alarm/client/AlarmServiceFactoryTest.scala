package csw.alarm.client
import akka.actor.CoordinatedShutdown.UnknownReason
import com.typesafe.config.ConfigFactory
import csw.commons.utils.SocketUtils.getFreePort
import csw.location.api.models.TcpRegistration
import csw.alarm.api.models.AlarmSeverity.Indeterminate
import csw.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.location.api.commons.ClusterAwareSettings
import csw.location.scaladsl.LocationServiceFactory
import csw.logging.commons.LogAdminActorFactory

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
    locationService.shutdown(UnknownReason).await
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
