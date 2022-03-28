/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.keyspace

import enumeratum.{Enum, EnumEntry}
import romaine.codec.RomaineCodec

import scala.collection.immutable

sealed abstract class RedisKeyspaceEvent(event: String) extends EnumEntry {
  override def entryName: String = event
}

object RedisKeyspaceEvent extends Enum[RedisKeyspaceEvent] {

  override def values: immutable.IndexedSeq[RedisKeyspaceEvent] = findValues

  implicit val codec: RomaineCodec[RedisKeyspaceEvent] =
    RomaineCodec.stringCodec.bimap(_.entryName, withNameInsensitiveOption(_).getOrElse(Unknown))

  case object Set     extends RedisKeyspaceEvent("set")
  case object Expired extends RedisKeyspaceEvent("expired")
  case object Delete  extends RedisKeyspaceEvent("del")
  case object Unknown extends RedisKeyspaceEvent("unknown")
}
