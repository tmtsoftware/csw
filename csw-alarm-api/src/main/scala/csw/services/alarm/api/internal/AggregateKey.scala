package csw.services.alarm.api.internal

import csw.services.alarm.api.internal.AggregateKey.KEYSPACE
import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

private[alarm] case class AggregateKey(key: String) {
  def toStatusKey: StatusKey = StatusKey(key.replace(KEYSPACE, ""))
}

private[alarm] object AggregateKey {
  val KEYSPACE = "__keyspace@0__:"

  // statusKey e.g = "status.nfiraos.*.*"
  implicit def fromAlarmKey(key: Key): AggregateKey = AggregateKey(KEYSPACE + StatusKey.fromAlarmKey(key))
}
