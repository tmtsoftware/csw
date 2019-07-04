package csw.command.client.cbor

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, TypedActorSystemOps, _}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.CommandResponseManagerMessage.{Query, Subscribe, Unsubscribe}
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.command.client.messages.ComponentCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.messages.{GetComponentLogMetadata, LogControlMessages, SetComponentLogLevel}
import csw.command.client.models.framework.ContainerLifecycleState.{Idle, Running}
import csw.command.client.models.framework.LockingResponses._
import csw.command.client.models.framework.PubSub.{Publish, PublisherMessage, SubscriberMessage}
import csw.command.client.models.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.command.client.models.framework._
import csw.location.api.codecs.LocationCodecs
import csw.logging.client.cbor.LoggingCodecs
import csw.params.core.formats.{CborHelpers, ParamCodecs}
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Codec, Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

trait MessageCodecs extends ParamCodecs with LoggingCodecs with LocationCodecs {

  implicit def actorSystem: ActorSystem[_]

  implicit override def actorRefCodec[T]: Codec[ActorRef[T]] =
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

  implicit lazy val durationCodec: Codec[FiniteDuration] =
    bimap[(Long, String), FiniteDuration]({
      case (length, unitStr) => FiniteDuration(length, TimeUnit.valueOf(unitStr))
    }, finiteDuration => (finiteDuration.length, finiteDuration.unit.toString))

  // ************************ LockingResponse Codecs ********************

  implicit lazy val lockReleasedCodec: Codec[LockReleased.type]               = deriveCodec[LockReleased.type]
  implicit lazy val lockExpiringShortlyCodec: Codec[LockExpiringShortly.type] = deriveCodec[LockExpiringShortly.type]
  implicit lazy val lockExpiredCodec: Codec[LockExpired.type]                 = deriveCodec[LockExpired.type]
  implicit lazy val lockAlreadyReleasedCodec: Codec[LockAlreadyReleased.type] = deriveCodec[LockAlreadyReleased.type]
  implicit lazy val lockAcquiredCodec: Codec[LockAcquired.type]               = deriveCodec[LockAcquired.type]
  implicit lazy val releasingLockFailedCodec: Codec[ReleasingLockFailed]      = deriveCodec[ReleasingLockFailed]
  implicit lazy val acquiringLockFailedCodec: Codec[AcquiringLockFailed]      = deriveCodec[AcquiringLockFailed]
  implicit lazy val lockingResponseCodec: Codec[LockingResponse]              = deriveCodec[LockingResponse]

  // ************************ SupervisorLifecycleState Codecs ********************
  implicit lazy val idleSLSCodec: Codec[SupervisorLifecycleState.Idle.type] = deriveCodec[SupervisorLifecycleState.Idle.type]
  implicit lazy val runningSLSCodec: Codec[SupervisorLifecycleState.Running.type] =
    deriveCodec[SupervisorLifecycleState.Running.type]
  implicit lazy val runningOfflineSLSCodec: Codec[SupervisorLifecycleState.RunningOffline.type] =
    deriveCodec[SupervisorLifecycleState.RunningOffline.type]
  implicit lazy val restartSLSCodec: Codec[SupervisorLifecycleState.Restart.type] =
    deriveCodec[SupervisorLifecycleState.Restart.type]
  implicit lazy val shutdownSLSCodec: Codec[SupervisorLifecycleState.Shutdown.type] =
    deriveCodec[SupervisorLifecycleState.Shutdown.type]
  implicit lazy val lockSLSCodec: Codec[SupervisorLifecycleState.Lock.type]        = deriveCodec[SupervisorLifecycleState.Lock.type]
  implicit lazy val supervisorLifecycleStateCodec: Codec[SupervisorLifecycleState] = deriveCodec[SupervisorLifecycleState]

  // ************************ Components Codecs ********************

  implicit lazy val locationServiceUsageCodec: Codec[LocationServiceUsage] = enumCodec[LocationServiceUsage]
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]               = deriveCodec[ComponentInfo]
  implicit lazy val componentCodec: Codec[Component]                       = deriveCodec[Component]
  implicit lazy val componentsCodec: Codec[Components]                     = deriveCodec[Components]

  // ************************ RemoteMsg Codecs ********************

  implicit lazy val shutdownCodec: Codec[Shutdown.type]  = deriveCodec[Shutdown.type]
  implicit lazy val restartCodec: Codec[Restart.type]    = deriveCodec[Restart.type]
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

  implicit lazy val idleCodec: Codec[Idle.type]                                        = deriveCodec[Idle.type]
  implicit lazy val runningCodec: Codec[Running.type]                                  = deriveCodec[Running.type]
  implicit lazy val containerLifecycleStateCodec: Codec[ContainerLifecycleState]       = deriveCodec[ContainerLifecycleState]
  implicit lazy val getContainerLifecycleStateCodec: Codec[GetContainerLifecycleState] = deriveCodec[GetContainerLifecycleState]

  implicit lazy val setComponentLogLevelCodec: Codec[SetComponentLogLevel]             = deriveCodec[SetComponentLogLevel]
  implicit lazy val getComponentLogMetadataCodec: Codec[GetComponentLogMetadata]       = deriveCodec[GetComponentLogMetadata]
  implicit lazy val logControlMessagesCodec: Codec[LogControlMessages]                 = deriveCodec[LogControlMessages]
  implicit lazy val componentStateSubscriptionCodec: Codec[ComponentStateSubscription] = deriveCodec[ComponentStateSubscription]
  implicit lazy val messageRemoteMsgCodec: Codec[RemoteMsg]                            = deriveCodec[RemoteMsg]

  // ************************ ToComponentLifecycleMessage Codecs ********************

  implicit lazy val goOfflineCodec: Codec[GoOffline.type] = deriveCodec[GoOffline.type]
  implicit lazy val goOnlineCodec: Codec[GoOnline.type]   = deriveCodec[GoOnline.type]
  implicit lazy val toComponentLifecycleMessagesCodec: Codec[ToComponentLifecycleMessage] =
    deriveCodec[ToComponentLifecycleMessage]

}
