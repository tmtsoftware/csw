package csw.command.client.cbor

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.Serializer
import csw.command.client.messages._
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.command.client.models.framework._
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import csw.params.commands.CommandResponse._
import csw.params.core.states.StateVariable
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

class CommandAkkaSerializer(_actorSystem: ExtendedActorSystem) extends Serializer with MessageCodecs {

  private val logger: Logger                        = GenericLoggerFactory.getLogger
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 19923

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: CommandSerializationMarker.RemoteMsg => Cbor.encode(x).toByteArray
    case x: ValidateResponse                     => Cbor.encode(x).toByteArray
    case x: SubmitResponse                       => Cbor.encode(x).toByteArray
    case x: OnewayResponse                       => Cbor.encode(x).toByteArray
    case x: MatchingResponse                     => Cbor.encode(x).toByteArray
    case x: ValidateCommandResponse              => Cbor.encode(x).toByteArray
    case x: StateVariable                        => Cbor.encode(x).toByteArray
    case x: SupervisorLifecycleState             => Cbor.encode(x).toByteArray
    case x: ContainerLifecycleState              => Cbor.encode(x).toByteArray
    case x: LifecycleStateChanged                => Cbor.encode(x).toByteArray
    case x: Components                           => Cbor.encode(x).toByteArray
    case x: LockingResponse                      => Cbor.encode(x).toByteArray
    case x: SubmitSequence                       => Cbor.encode(x).toByteArray
    case x: Query                                => Cbor.encode(x).toByteArray
    case x: QueryFinal                           => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    def fromBinary[T: ClassTag: Decoder]: Option[T] = {
      val clazz = scala.reflect.classTag[T].runtimeClass
      if (clazz.isAssignableFrom(manifest.get)) Some(Cbor.decode(bytes).to[T].value)
      else None
    }

    {
      fromBinary[SubmitResponse] orElse
      fromBinary[OnewayResponse] orElse
      fromBinary[ValidateResponse] orElse
      fromBinary[ValidateCommandResponse] orElse
      fromBinary[MatchingResponse] orElse
      fromBinary[StateVariable] orElse
      fromBinary[CommandSerializationMarker.RemoteMsg] orElse
      fromBinary[SupervisorLifecycleState] orElse
      fromBinary[ContainerLifecycleState] orElse
      fromBinary[LifecycleStateChanged] orElse
      fromBinary[Components] orElse
      fromBinary[LockingResponse] orElse
      fromBinary[SubmitSequence] orElse
      fromBinary[QueryFinal] orElse
      fromBinary[Query]
    } getOrElse {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
