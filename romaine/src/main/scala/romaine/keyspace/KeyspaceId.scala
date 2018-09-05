package romaine.keyspace

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class KeyspaceId(val id: String) extends EnumEntry {
  override def entryName = s"__keyspace@${id}__:"
}

object KeyspaceId extends Enum[KeyspaceId] {
  override def values: immutable.IndexedSeq[KeyspaceId] = findValues

  case object _0 extends KeyspaceId("0")
  case object _1 extends KeyspaceId("1")
}
