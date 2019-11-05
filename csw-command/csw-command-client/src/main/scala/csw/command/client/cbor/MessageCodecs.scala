package csw.command.client.cbor

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.command.client.messages.ComponentCommonMessage._
import csw.command.client.messages.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.command.client.messages.DiagnosticDataMessage.{DiagnosticMode, OperationsMode}
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.messages._
import csw.command.client.messages.sequencer.SequencerMsg.{QueryFinal, SubmitSequenceAndWait}
import csw.command.client.models.framework.LockingResponse._
import csw.command.client.models.framework.PubSub.{Publish, PublisherMessage, SubscriberMessage}
import csw.command.client.models.framework.{PubSub, _}
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Codec, Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

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

  implicit def subscribeMessageCodec[T: Encoder: Decoder]: Codec[PubSub.Subscribe[T]]         = deriveCodec
  implicit def subscribeOnlyMessageCodec[T: Encoder: Decoder]: Codec[PubSub.SubscribeOnly[T]] = deriveCodec
  implicit def unsubscribeMessageCodec[T: Encoder: Decoder]: Codec[PubSub.Unsubscribe[T]]     = deriveCodec
  implicit def subscriberMessageCodec[T: Encoder: Decoder]: Codec[SubscriberMessage[T]]       = deriveCodec
  implicit def publishCodec[T: Encoder: Decoder]: Codec[Publish[T]]                           = deriveCodec
  implicit def publisherMessageCodec[T: Encoder: Decoder]: Codec[PublisherMessage[T]]         = deriveCodec
  implicit def pubSubCodec[T: Encoder: Decoder]: Codec[PubSub[T]]                             = deriveCodec

  implicit lazy val durationCodec: Codec[FiniteDuration] = Codec.bimap[(Long, String), FiniteDuration](
    finiteDuration => (finiteDuration.length, finiteDuration.unit.toString),
    { case (length, unitStr) => FiniteDuration(length, TimeUnit.valueOf(unitStr)) }
  )

  // ************************ LockingResponse Codecs ********************

  implicit lazy val lockReleasedCodec: Codec[LockReleased.type]               = deriveCodec
  implicit lazy val lockExpiringShortlyCodec: Codec[LockExpiringShortly.type] = deriveCodec
  implicit lazy val lockExpiredCodec: Codec[LockExpired.type]                 = deriveCodec
  implicit lazy val lockAlreadyReleasedCodec: Codec[LockAlreadyReleased.type] = deriveCodec
  implicit lazy val lockAcquiredCodec: Codec[LockAcquired.type]               = deriveCodec
  implicit lazy val releasingLockFailedCodec: Codec[ReleasingLockFailed]      = deriveUnaryCodec
  implicit lazy val acquiringLockFailedCodec: Codec[AcquiringLockFailed]      = deriveUnaryCodec
  implicit lazy val lockingResponseCodec: Codec[LockingResponse]              = deriveCodec

  // ************************ Components Codecs ********************

  implicit lazy val componentInfoCodec: Codec[ComponentInfo] = deriveCodec
  implicit lazy val componentCodec: Codec[Component]         = deriveCodec
  implicit lazy val componentsCodec: Codec[Components]       = deriveCodec

  // ************************ RemoteMsg Codecs ********************

  implicit lazy val shutdownCodec: Codec[Shutdown.type]                                  = deriveCodec
  implicit lazy val restartCodec: Codec[Restart.type]                                    = deriveCodec
  implicit lazy val queryCodec: Codec[Query]                                             = deriveCodec
  implicit lazy val subscribeCodec: Codec[Subscribe]                                     = deriveCodec
  implicit lazy val unsubscribeCodec: Codec[Unsubscribe]                                 = deriveCodec
  implicit lazy val submitCodec: Codec[Submit]                                           = deriveCodec
  implicit lazy val oneWayCodec: Codec[Oneway]                                           = deriveCodec
  implicit lazy val validateCodec: Codec[Validate]                                       = deriveCodec
  implicit lazy val lockCodec: Codec[Lock]                                               = deriveCodec
  implicit lazy val unlockCodec: Codec[Unlock]                                           = deriveCodec
  implicit lazy val lifecycleCodec: Codec[Lifecycle]                                     = deriveCodec
  implicit lazy val diagnosticModeCodec: Codec[DiagnosticMode]                           = deriveCodec
  implicit lazy val operationsModeCodec: Codec[OperationsMode.type]                      = deriveCodec
  implicit lazy val lifecycleStateChangedCodec: Codec[LifecycleStateChanged]             = deriveCodec
  implicit lazy val lifecycleStateSubscriptionCodec: Codec[LifecycleStateSubscription]   = deriveCodec
  implicit lazy val getContainerLifecycleStateCodec: Codec[GetContainerLifecycleState]   = deriveCodec
  implicit lazy val getSupervisorLifecycleStateCodec: Codec[GetSupervisorLifecycleState] = deriveCodec
  implicit lazy val setComponentLogLevelCodec: Codec[SetComponentLogLevel]               = deriveCodec
  implicit lazy val getComponentLogMetadataCodec: Codec[GetComponentLogMetadata]         = deriveCodec
  implicit lazy val getComponentsCodec: Codec[GetComponents]                             = deriveCodec
  implicit lazy val logControlMessageCodec: Codec[LogControlMessage]                     = deriveCodec
  implicit lazy val componentStateSubscriptionCodec: Codec[ComponentStateSubscription]   = deriveCodec
  implicit lazy val messageRemoteMsgCodec: Codec[RemoteMsg]                              = deriveCodec

  // ************************ SequencerMsg Codecs ********************

  implicit lazy val submitSequenceAndWaitCodec: Codec[SubmitSequenceAndWait] = deriveCodec
  implicit lazy val queryFinalCodec: Codec[QueryFinal]                       = deriveUnaryCodec
}
