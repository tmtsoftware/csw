package csw.services.alarm.api.internal

import csw.services.alarm.api.internal.AggregateKey.KEYSPACE
import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

case class AggregateKey(key: String) {
  def toStatusKey: StatusKey = StatusKey(key.replace(KEYSPACE, ""))
}

object AggregateKey {
  val KEYSPACE = "__keyspace@0__:"

  // statusKey e.g = "status.nfiraos.*.*"
  implicit def fromAlarmKey(key: Key): AggregateKey = AggregateKey(KEYSPACE + StatusKey.fromAlarmKey(key))
}
