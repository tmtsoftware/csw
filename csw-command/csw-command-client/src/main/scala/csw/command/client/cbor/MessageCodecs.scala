package csw.command.client.cbor

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.CommandResponseManagerMessage.{Query, Subscribe, Unsubscribe}
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.command.client.messages.ComponentCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorLifecycleState,
  LifecycleStateSubscription
}
import csw.command.client.messages.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.messages.{GetComponentLogMetadata, LogControlMessages, SetComponentLogLevel}
import csw.command.client.models.framework.LockingResponse._
import csw.command.client.models.framework.PubSub.{Publish, PublisherMessage, SubscriberMessage}
import csw.command.client.models.framework.{PubSub, _}
import csw.location.models.codecs.LocationCodecs
import csw.logging.client.cbor.LoggingCodecs
import csw.params.core.formats.{CborHelpers, ParamCodecs}
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass
import io.bullet.borer.{Codec, Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

trait MessageCodecs extends ParamCodecs with LoggingCodecs with LocationCodecs {

  implicit def actorSystem: ActorSystem[_]

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] =
    CborHelpers.bimap[String, ActorRef[T]](
      path => {
        val provider = SerializationExtension(actorSystem.toUntyped).system.provider
        provider.resolveActorRef(path)
      },
      actorRef => Serialization.serializedActorPath(actorRef.toUntyped)
    )

  implicit def subscribeMessageCodec[T: Encoder: Decoder]: Codec[PubSub.Subscribe[T]] = deriveCodec[PubSub.Subscribe[T]]
  implicit def subscribeOnlyMessageCodec[T: Encoder: Decoder]: Codec[PubSub.SubscribeOnly[T]] =
    deriveCodec[PubSub.SubscribeOnly[T]]
  implicit def unsubscribeMessageCodec[T: Encoder: Decoder]: Codec[PubSub.Unsubscribe[T]] = deriveCodec[PubSub.Unsubscribe[T]]
  implicit def subscriberMessageCodec[T: Encoder: Decoder]: Codec[SubscriberMessage[T]]   = deriveCodec[SubscriberMessage[T]]
  implicit def publishCodec[T: Encoder: Decoder]: Codec[Publish[T]]                       = deriveCodec[Publish[T]]
  implicit def publisherMessageCodec[T: Encoder: Decoder]: Codec[PublisherMessage[T]]     = deriveCodec[PublisherMessage[T]]
  implicit def pubSubCodec[T: Encoder: Decoder]: Codec[PubSub[T]]                         = deriveCodec[PubSub[T]]

  implicit lazy val durationCodec: Codec[FiniteDuration] =
    bimap[(Long, String), FiniteDuration]({
      case (length, unitStr) => FiniteDuration(length, TimeUnit.valueOf(unitStr))
    }, finiteDuration => (finiteDuration.length, finiteDuration.unit.toString))

  // ************************ LockingResponse Codecs ********************

  def singletonCodec[T <: Singleton](a: T): Codec[T] = CborHelpers.bimap[String, T](_ => a, _.toString)

  implicit lazy val lockReleasedCodec: Codec[LockReleased.type]               = singletonCodec(LockReleased)
  implicit lazy val lockExpiringShortlyCodec: Codec[LockExpiringShortly.type] = singletonCodec(LockExpiringShortly)
  implicit lazy val lockExpiredCodec: Codec[LockExpired.type]                 = singletonCodec(LockExpired)
  implicit lazy val lockAlreadyReleasedCodec: Codec[LockAlreadyReleased.type] = singletonCodec(LockAlreadyReleased)
  implicit lazy val lockAcquiredCodec: Codec[LockAcquired.type]               = singletonCodec(LockAcquired)
  implicit lazy val releasingLockFailedCodec: Codec[ReleasingLockFailed]      = deriveCodecForUnaryCaseClass[ReleasingLockFailed]
  implicit lazy val acquiringLockFailedCodec: Codec[AcquiringLockFailed]      = deriveCodecForUnaryCaseClass[AcquiringLockFailed]
  implicit lazy val lockingResponseCodec: Codec[LockingResponse]              = deriveCodec[LockingResponse]

  // ************************ Components Codecs ********************

  implicit lazy val locationServiceUsageCodec: Codec[LocationServiceUsage] = enumCodec[LocationServiceUsage]
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]               = deriveCodec[ComponentInfo]
  implicit lazy val componentCodec: Codec[Component]                       = deriveCodec[Component]
  implicit lazy val componentsCodec: Codec[Components]                     = deriveCodec[Components]

  // ************************ RemoteMsg Codecs ********************

  implicit lazy val shutdownCodec: Codec[Shutdown.type]  = singletonCodec(Shutdown)
  implicit lazy val restartCodec: Codec[Restart.type]    = singletonCodec(Restart)
  implicit lazy val queryCodec: Codec[Query]             = deriveCodec[Query]
  implicit lazy val subscribeCodec: Codec[Subscribe]     = deriveCodec[Subscribe]
  implicit lazy val unsubscribeCodec: Codec[Unsubscribe] = deriveCodec[Unsubscribe]
  implicit lazy val submitCodec: Codec[Submit]           = deriveCodec[Submit]
  implicit lazy val oneWayCodec: Codec[Oneway]           = deriveCodec[Oneway]
  implicit lazy val validateCodec: Codec[Validate]       = deriveCodec[Validate]
  implicit lazy val lockCodec: Codec[Lock]               = deriveCodec[Lock]
  implicit lazy val unlockCodec: Codec[Unlock]           = deriveCodec[Unlock]
  implicit lazy val lifecycleCodec: Codec[Lifecycle]     = deriveCodec[Lifecycle]

  implicit lazy val lifecycleStateChangedCodec: Codec[LifecycleStateChanged]           = deriveCodec[LifecycleStateChanged]
  implicit lazy val lifecycleStateSubscriptionCodec: Codec[LifecycleStateSubscription] = deriveCodec[LifecycleStateSubscription]

  implicit lazy val containerLifecycleStateCodec: Codec[ContainerLifecycleState]          = enumCodec[ContainerLifecycleState]
  implicit lazy val supervisorLifecycleStateCodec: Codec[SupervisorLifecycleState]        = enumCodec[SupervisorLifecycleState]
  implicit lazy val toComponentLifecycleMessagesCodec: Codec[ToComponentLifecycleMessage] = enumCodec[ToComponentLifecycleMessage]

  implicit lazy val getContainerLifecycleStateCodec: Codec[GetContainerLifecycleState] = deriveCodec[GetContainerLifecycleState]
  implicit lazy val getSupervisorLifecycleStateCodec: Codec[GetSupervisorLifecycleState] =
    deriveCodec[GetSupervisorLifecycleState]
  implicit lazy val setComponentLogLevelCodec: Codec[SetComponentLogLevel]             = deriveCodec[SetComponentLogLevel]
  implicit lazy val getComponentLogMetadataCodec: Codec[GetComponentLogMetadata]       = deriveCodec[GetComponentLogMetadata]
  implicit lazy val getComponentsCodec: Codec[GetComponents]                           = deriveCodec[GetComponents]
  implicit lazy val logControlMessagesCodec: Codec[LogControlMessages]                 = deriveCodec[LogControlMessages]
  implicit lazy val componentStateSubscriptionCodec: Codec[ComponentStateSubscription] = deriveCodec[ComponentStateSubscription]
  implicit lazy val messageRemoteMsgCodec: Codec[RemoteMsg]                            = deriveCodec[RemoteMsg]
}
