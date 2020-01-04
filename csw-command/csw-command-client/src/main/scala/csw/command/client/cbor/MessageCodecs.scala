package csw.command.client.cbor

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.command.client.messages.sequencer.SequencerMsg.{
  SubmitSequence,
  Query => SequencerQuery,
  QueryFinal => SequencerQueryFinal
}
import csw.command.client.models.framework.PubSub.{Publish, PublisherMessage, SubscriberMessage}
import csw.command.client.models.framework.{PubSub, _}
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.derivation.CompactMapBasedCodecs._
import io.bullet.borer.{Codec, Decoder, Encoder}

trait MessageCodecs extends ParamCodecs with LoggingCodecs with LocationCodecs {

  implicit def actorSystem: ActorSystem[_]

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] =
    Codec.bimap[String, ActorRef[T]](
      actorRef => Serialization.serializedActorPath(actorRef.toClassic),
      path => {
        val provider = SerializationExtension(actorSystem.toClassic).system.provider
        provider.resolveActorRef(path)
      }
    )

  //todo: deriveAllCodecs
  implicit def subscribeMessageCodec[T: Encoder: Decoder]: Codec[PubSub.Subscribe[T]]         = deriveCodec
  implicit def subscribeOnlyMessageCodec[T: Encoder: Decoder]: Codec[PubSub.SubscribeOnly[T]] = deriveCodec
  implicit def unsubscribeMessageCodec[T: Encoder: Decoder]: Codec[PubSub.Unsubscribe[T]]     = deriveCodec
  implicit def subscriberMessageCodec[T: Encoder: Decoder]: Codec[SubscriberMessage[T]]       = deriveCodec
  implicit def publishCodec[T: Encoder: Decoder]: Codec[Publish[T]]                           = deriveCodec
  implicit def publisherMessageCodec[T: Encoder: Decoder]: Codec[PublisherMessage[T]]         = deriveCodec
  implicit def pubSubCodec[T: Encoder: Decoder]: Codec[PubSub[T]]                             = deriveCodec

  // ************************ LockingResponse Codecs ********************

  implicit lazy val lockingResponseCodec: Codec[LockingResponse] = deriveAllCodecs

  // ************************ Components Codecs ********************

  implicit lazy val componentInfoCodec: Codec[ComponentInfo] = deriveCodec
  implicit lazy val componentCodec: Codec[Component]         = deriveCodec
  implicit lazy val componentsCodec: Codec[Components]       = deriveCodec

  // ************************ RemoteMsg Codecs ********************

  implicit lazy val messageRemoteMsgCodec: Codec[RemoteMsg]                  = deriveAllCodecs
  implicit lazy val lifecycleStateChangedCodec: Codec[LifecycleStateChanged] = deriveCodec

  // ************************ SequencerMsg Codecs ********************

  implicit lazy val submitSequenceCodec: Codec[SubmitSequence]           = deriveCodec
  implicit lazy val sequencerQueryFinalCodec: Codec[SequencerQueryFinal] = deriveCodec
  implicit lazy val sequencerQueryCodec: Codec[SequencerQuery]           = deriveCodec
}
