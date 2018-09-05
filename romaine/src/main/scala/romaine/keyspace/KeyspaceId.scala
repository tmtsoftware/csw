package romaine.keyspace

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class KeyspaceId(val id: String) extends EnumEntry {
  override def entryName = s"__keyspace@${id}__"
}

/**
 * By default, redis has 16 databases which are identified by id starting from 0
 * */
object KeyspaceId extends Enum[KeyspaceId] {
  override def values: immutable.IndexedSeq[KeyspaceId] = findValues

  case object _0  extends KeyspaceId("0")
  case object _1  extends KeyspaceId("1")
  case object _2  extends KeyspaceId("2")
  case object _3  extends KeyspaceId("3")
  case object _4  extends KeyspaceId("4")
  case object _5  extends KeyspaceId("5")
  case object _6  extends KeyspaceId("6")
  case object _7  extends KeyspaceId("7")
  case object _8  extends KeyspaceId("8")
  case object _9  extends KeyspaceId("9")
  case object _10 extends KeyspaceId("10")
  case object _11 extends KeyspaceId("11")
  case object _12 extends KeyspaceId("12")
  case object _13 extends KeyspaceId("13")
  case object _14 extends KeyspaceId("14")
  case object _15 extends KeyspaceId("15")
}
