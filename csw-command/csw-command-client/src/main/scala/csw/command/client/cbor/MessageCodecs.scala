package csw.command.client.cbor

import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, TypedActorSystemOps, _}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.command.client.models.framework.PubSub.{Publish, PublisherMessage, SubscriberMessage}
import csw.command.client.models.framework.{LifecycleStateChanged, PubSub, SupervisorLifecycleState, ToComponentLifecycleMessage}
import csw.logging.client.cbor.LoggingCodecs
import csw.params.core.formats.{CborHelpers, ParamCodecs}
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Codec, Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

trait MessageCodecs extends ParamCodecs with LoggingCodecs {

  implicit def actorSystem: ActorSystem[_]

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] =
    CborHelpers.bimap[String, ActorRef[T]](
      path => {
        val provider = SerializationExtension(actorSystem.toUntyped).system.provider
        provider.resolveActorRef(path)
      },
      actorRef => Serialization.serializedActorPath(actorRef.toUntyped)
    )

  implicit def subscriberMessageCodec[T: Encoder: Decoder]: Codec[SubscriberMessage[T]] = deriveCodec[SubscriberMessage[T]]
  implicit def publishCodec[T: Encoder: Decoder]: Codec[Publish[T]]                     = deriveCodec[Publish[T]]
  implicit def publisherMessageCodec[T: Encoder: Decoder]: Codec[PublisherMessage[T]]   = deriveCodec[PublisherMessage[T]]
  implicit def pubSubCodec[T: Encoder: Decoder]: Codec[PubSub[T]]                       = deriveCodec[PubSub[T]]

  implicit lazy val lifecycleStateChangedCodec: Codec[LifecycleStateChanged]       = deriveCodec[LifecycleStateChanged]
  implicit lazy val supervisorLifecycleStateCodec: Codec[SupervisorLifecycleState] = deriveCodec[SupervisorLifecycleState]
  implicit lazy val durationCodec: Codec[FiniteDuration] =
    bimap[(Long, String), FiniteDuration]({
      case (length, unitStr) => FiniteDuration(length, unitStr)
    }, finiteDuration => (finiteDuration.length, finiteDuration.unit.toString))

  implicit lazy val toComponentLifecycleMessageCodec: Codec[ToComponentLifecycleMessage] =
    deriveCodec[ToComponentLifecycleMessage]
  implicit lazy val messageRemoteMsgCodec: Codec[RemoteMsg] = deriveCodec[RemoteMsg]

}
