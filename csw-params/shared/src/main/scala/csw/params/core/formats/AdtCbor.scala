/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.formats

import csw.params.commands.{CommandIssue, SequenceCommand}
import csw.params.core.formats.ParamCodecs.*
import csw.params.events.Event
import io.bullet.borer.{Cbor, Decoder, Encoder}

abstract class AdtCbor[T: Encoder: Decoder] {
  def encode(adt: T): Array[Byte]           = Cbor.encode(adt).toByteArray
  def decode[U <: T](bytes: Array[Byte]): U = Cbor.decode(bytes).to[T].value.asInstanceOf[U]
}

object EventCbor        extends AdtCbor[Event]
object CommandCbor      extends AdtCbor[SequenceCommand]
object CommandIssueCbor extends AdtCbor[CommandIssue]
