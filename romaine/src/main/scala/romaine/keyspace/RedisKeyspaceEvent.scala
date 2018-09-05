package romaine.keyspace

import enumeratum.{Enum, EnumEntry}
import romaine.codec.RomaineStringCodec

import scala.collection.immutable

sealed abstract class RedisKeyspaceEvent(event: String) extends EnumEntry {
  override def entryName: String = event
}

object RedisKeyspaceEvent extends Enum[RedisKeyspaceEvent] {

  override def values: immutable.IndexedSeq[RedisKeyspaceEvent] = findValues

  implicit val codec: RomaineStringCodec[RedisKeyspaceEvent] =
    RomaineStringCodec.codec(_.entryName, withNameInsensitiveOption(_).getOrElse(Unknown))

  case object Set     extends RedisKeyspaceEvent("set")
  case object Expired extends RedisKeyspaceEvent("expired")
  case object Delete  extends RedisKeyspaceEvent("del")
  case object Unknown extends RedisKeyspaceEvent("unknown")
}
