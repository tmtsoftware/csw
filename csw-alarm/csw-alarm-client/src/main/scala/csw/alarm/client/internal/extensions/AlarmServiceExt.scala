package csw.alarm.client.internal.extensions

import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.internal.JAlarmServiceImpl

object AlarmServiceExt {

  implicit class RichAlarmService(val alarmService: AlarmService) {
    def asJava: JAlarmServiceImpl = new JAlarmServiceImpl(alarmService)
  }

}
