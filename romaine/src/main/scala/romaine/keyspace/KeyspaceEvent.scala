package romaine.keyspace

import enumeratum.{Enum, EnumEntry}
import romaine.codec.RomaineStringCodec

import scala.collection.immutable

sealed abstract class KeyspaceEvent(event: String) extends EnumEntry {
  override def entryName: String = event
}

object KeyspaceEvent extends Enum[KeyspaceEvent] {

  override def values: immutable.IndexedSeq[KeyspaceEvent] = findValues

  implicit val codec: RomaineStringCodec[KeyspaceEvent] =
    RomaineStringCodec.codec(_.entryName, withNameInsensitiveOption(_).getOrElse(Unknown))

  case object Set     extends KeyspaceEvent("set")
  case object Expired extends KeyspaceEvent("expired")
  case object Delete  extends KeyspaceEvent("del")
  case object Unknown extends KeyspaceEvent("unknown")
}
