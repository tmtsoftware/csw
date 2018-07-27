package csw.services.alarm.client.internal.shelve
import csw.services.alarm.api.models.Key.AlarmKey

sealed trait ShelveTimeoutMessage

object ShelveTimeoutMessage {
  final case class ScheduleShelveTimeout(key: AlarmKey) extends ShelveTimeoutMessage
  final case class CancelShelveTimeout(key: AlarmKey)   extends ShelveTimeoutMessage
  final case class ShelveHasTimedOut(key: AlarmKey)     extends ShelveTimeoutMessage
}
