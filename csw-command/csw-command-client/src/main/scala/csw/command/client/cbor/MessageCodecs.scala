/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.cbor

import org.apache.pekko.actor.typed.ActorRef
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.command.client.messages.sequencer.CswSequencerMessage
import csw.command.client.models.framework.*
import csw.location.api.codec.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import io.bullet.borer.{Codec, Decoder, Encoder}

trait MessageCodecs extends MessageCodecsBase {
  implicit def pubSubCodec[T: Encoder: Decoder, PS[_] <: PubSub[_]]: Codec[PS[T]] = pubSubCodecValue[T].asInstanceOf[Codec[PS[T]]]
  implicit lazy val messageRemoteMsgCodec: Codec[RemoteMsg]                       = deriveAllCodecs
}

trait MessageCodecsBase extends ParamCodecs with LoggingCodecs with LocationCodecs {
  implicit def actorRefCodec[T]: Codec[ActorRef[T]] = io.bullet.borer.compat.pekko.typedActorRefCodec

  protected def pubSubCodecValue[T: Encoder: Decoder]: Codec[PubSub[T]] = deriveAllCodecs

  // ************************ LockingResponse Codecs ********************

  implicit lazy val lockingResponseCodec: Codec[LockingResponse] = deriveAllCodecs

  // ************************ Components Codecs ********************

  implicit lazy val componentInfoCodec: Codec[ComponentInfo] = deriveCodec
  implicit lazy val componentCodec: Codec[Component]         = deriveCodec
  implicit lazy val componentsCodec: Codec[Components]       = deriveCodec

  // ************************ RemoteMsg Codecs ********************

  implicit lazy val lifecycleStateChangedCodec: Codec[LifecycleStateChanged] = deriveCodec

  // ************************ SequencerMsg Codecs ********************

  implicit lazy val cswSequencerMessageCodec: Codec[CswSequencerMessage] = deriveAllCodecs
}
