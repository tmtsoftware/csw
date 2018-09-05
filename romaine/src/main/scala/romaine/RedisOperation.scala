package romaine
import enumeratum.{Enum, EnumEntry}
import romaine.codec.RomaineStringCodec

import scala.collection.immutable

sealed abstract class RedisOperation(operation: String) extends EnumEntry {
  override def entryName: String = operation
}

object RedisOperation extends Enum[RedisOperation] {

  override def values: immutable.IndexedSeq[RedisOperation] = findValues

  implicit val codec: RomaineStringCodec[RedisOperation] =
    RomaineStringCodec.codec(_.entryName, withNameInsensitiveOption(_).getOrElse(Unknown))

  case object Set     extends RedisOperation("set")
  case object Expired extends RedisOperation("expired")
  case object Delete  extends RedisOperation("del")
  case object Unknown extends RedisOperation("unknown")
}
