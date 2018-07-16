package csw.services.alarm.client.internal

import csw.services.alarm.api.models.AlarmKey

sealed trait AlarmTimeoutMessage

object AlarmTimeoutMessage {
  case class ScheduleShelveTimeout(key: AlarmKey) extends AlarmTimeoutMessage
  case class CancelShelveTimeout(key: AlarmKey)   extends AlarmTimeoutMessage
  case class ShelveHasTimedOut(key: AlarmKey)     extends AlarmTimeoutMessage
}
