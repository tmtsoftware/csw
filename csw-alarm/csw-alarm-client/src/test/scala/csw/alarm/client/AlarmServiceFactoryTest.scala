/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client
import com.typesafe.config.ConfigFactory
import csw.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.alarm.models.AlarmSeverity.Indeterminate
import csw.location.api.models
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService

// DEOPSCSW-481: Component Developer API available to all CSW components
class AlarmServiceFactoryTest extends AlarmServiceTestSetup with HTTPLocationService {

  private val locationService = HttpLocationServiceFactory.makeLocalClient

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService.register(models.TcpRegistration(AlarmServiceConnection.value, sentinelPort)).await
  }

  override def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    alarmService.initAlarms(validAlarmsConfig, reset = true).await
  }

  override def afterAll(): Unit = super.afterAll()

  test("should create admin alarm service using location service | DEOPSCSW-481") {
    val alarmServiceUsingLS: AlarmAdminService = alarmServiceFactory.makeAdminApi(locationService)
    alarmServiceUsingLS.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldEqual tromboneAxisHighLimitAlarm
  }

  test("should create admin alarm service using host and port | DEOPSCSW-481") {
    val alarmServiceUsingHostAndPort: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort)
    alarmServiceUsingHostAndPort.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldEqual tromboneAxisHighLimitAlarm
  }

  test("should create client alarm service using location service | DEOPSCSW-481") {
    val alarmServiceUsingLS: AlarmService = alarmServiceFactory.makeClientApi(locationService)
    alarmServiceUsingLS.setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldEqual Indeterminate
  }

  test("should create client alarm service using host and port | DEOPSCSW-481") {
    val alarmServiceUsingUsingHostAndPort: AlarmService = alarmServiceFactory.makeClientApi(hostname, sentinelPort)
    alarmServiceUsingUsingHostAndPort.setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldEqual Indeterminate
  }

}
