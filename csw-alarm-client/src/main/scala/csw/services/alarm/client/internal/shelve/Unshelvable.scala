package csw.services.alarm.client.internal.shelve
import csw.services.alarm.api.models.Key.AlarmKey

trait Unshelvable {
  def unshelve(key: AlarmKey): Unit
}
