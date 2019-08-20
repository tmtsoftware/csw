package csw.params.core.formats

import java.lang.{Byte => JByte}
import java.time.Instant

import csw.params.commands.CommandIssue._
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Coords._
import csw.params.core.models._
import csw.params.core.states.{CurrentState, DemandState, StateName, StateVariable}
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TAITime, UTCTime}
import io.bullet.borer._
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.collection.mutable.{ArraySeq => ArrayS}
import scala.reflect.ClassTag

object JParamCodecs extends ParamCodecs
object ParamCodecs extends ParamCodecs

/**
 * Supports (de)serialization of csw models
 */
trait ParamCodecs extends CommonCodecs {

  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  def singletonCodec[T <: Singleton](a: T): Codec[T] = Codec.bimap[String, T](_.toString, _ => a)

  // ************************ Base Type Codecs ********************

  implicit lazy val choiceCodec: Codec[Choice] = deriveUnaryCodec[Choice]
  implicit lazy val raDecCodec: Codec[RaDec] = deriveCodec[RaDec]

  implicit lazy val tagCodec: Codec[Coords.Tag] = deriveUnaryCodec[Coords.Tag]
  implicit lazy val angleCodec: Codec[Angle] = deriveUnaryCodec[Angle]
  implicit lazy val properMotionCodec: Codec[ProperMotion] = deriveCodec[ProperMotion]
  implicit lazy val eqFrameCodec: Codec[EqFrame] = CodecHelpers.enumCodec[EqFrame]
  implicit lazy val solarSystemObjectCodec: Codec[SolarSystemObject] = CodecHelpers.enumCodec[SolarSystemObject]

  implicit lazy val eqCoordCodec: Codec[EqCoord] = deriveCodec[EqCoord]
  implicit lazy val solarSystemCoordCodec: Codec[SolarSystemCoord] = deriveCodec[SolarSystemCoord]
  implicit lazy val minorPlanetCoordCodec: Codec[MinorPlanetCoord] = deriveCodec[MinorPlanetCoord]
  implicit lazy val cometCoordCodec: Codec[CometCoord] = deriveCodec[CometCoord]
  implicit lazy val altAzCoordCodec: Codec[AltAzCoord] = deriveCodec[AltAzCoord]
  implicit lazy val coordCodec: Codec[Coord] = deriveCodec[Coord]

  implicit lazy val tsCodec: Codec[Timestamp] = deriveCodec[Timestamp]

  implicit lazy val instantEnc: Encoder[Instant] = Encoder.targetSpecific(
    cbor = tsCodec.encoder.contramap(Timestamp.fromInstant),
    json = Encoder.forString.contramap(_.toString)
  )

  implicit lazy val instantDec: Decoder[Instant] = Decoder.targetSpecific(
    cbor = tsCodec.decoder.map(_.toInstant),
    json = Decoder.forString.map(Instant.parse)
  )

  implicit lazy val utcTimeCodec: Codec[UTCTime] = deriveUnaryCodec[UTCTime]
  implicit lazy val taiTimeCodec: Codec[TAITime] = deriveUnaryCodec[TAITime]

  // ************************ Composite Codecs ********************

  implicit def arrayDataCodec[T: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    Codec.bimap[ArrayS[T], ArrayData[T]](_.data, ArrayData(_))

  implicit def matrixDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    Codec.bimap[ArrayS[ArrayS[T]], MatrixData[T]](_.data, MatrixData(_))(Encoder.forIterable, Decoder.forIterable)

  // ************************ Enum Codecs ********************

  implicit lazy val unitsCodec: Codec[Units] = CodecHelpers.enumCodec[Units]
  implicit lazy val keyTypeCodecExistential: Codec[KeyType[_]] = CodecHelpers.enumCodec[KeyType[_]]

  implicit def keyTypeCodec[T]: Codec[KeyType[T]] = keyTypeCodecExistential.asInstanceOf[Codec[KeyType[T]]]

  // ************************ Parameter Codecs ********************

  //Do not replace these with bimap, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val javaByteArrayEnc: Encoder[Array[JByte]] = bytesEnc.contramap(_.map(x => x: Byte))
  implicit lazy val javaByteArrayDec: Decoder[Array[JByte]] = bytesDec.map(_.map(x => x: JByte))

  implicit def waCodec[T: ArrayEnc: ArrayDec]: Codec[ArrayS[T]] =
    Codec.bimap[Array[T], ArrayS[T]](_.array.asInstanceOf[Array[T]], x => x: ArrayS[T])

  //Do not put the bytesEnc and bytesDec inside Codec, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val bytesEnc: Encoder[Array[Byte]] = Encoder.targetSpecific(
    cbor = Encoder.forByteArrayDefault,
    json = Encoder.forArray[Byte]
  )

  implicit lazy val bytesDec: Decoder[Array[Byte]] = Decoder.targetSpecific(
    cbor = Decoder.forByteArrayDefault,
    json = Decoder.forArray[Byte]
  )

  implicit def paramCoreCodec[T: ArrayEnc: ArrayDec]: Codec[ParamCore[T]] = deriveCodec[ParamCore[T]]

  implicit def paramCodec[T: ArrayEnc: ArrayDec]: Codec[Parameter[T]] =
    Codec.bimap[Map[String, ParamCore[T]], Parameter[T]](ParamCore.fromParam, ParamCore.toParam)

  implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    val encoder: Encoder[Parameter[Any]] = value.keyType.paramEncoder.asInstanceOf[Encoder[Parameter[Any]]]
    encoder.write(w, value.asInstanceOf[Parameter[Any]])
  }

  implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    r.tryReadMapHeader(1) || r.tryReadMapStart() || r.tryReadArrayHeader(1) || r.tryReadArrayStart()
    val keyTypeName = r.readString()
    val keyType = KeyType.withNameInsensitive(keyTypeName)
    val paramCore = keyType.paramCoreDecoder.read(r)
    if (r.target != Cbor) {
      r.tryReadBreak()
    }
    ParamCore.toParam(Map(keyTypeName -> paramCore))
  }

  // ************************ Struct Codecs ********************

  implicit lazy val structCodec: Codec[Struct] = deriveCodec[Struct]

  // ************************ Event Codecs ********************

  implicit lazy val idCodec: Codec[Id] = deriveUnaryCodec[Id]
  implicit lazy val eventNameCodec: Codec[EventName] = deriveUnaryCodec[EventName]
  private[formats] implicit lazy val seCodec: Codec[SystemEvent] = deriveCodec[SystemEvent]
  private[formats] implicit lazy val oeCodec: Codec[ObserveEvent] = deriveCodec[ObserveEvent]
  implicit lazy val eventCodec: Codec[Event] = deriveCodec[Event]

  // ************************ Command Codecs ********************

  implicit lazy val commandNameCodec: Codec[CommandName] = deriveUnaryCodec[CommandName]
  implicit lazy val obsIdCodec: Codec[ObsId] = deriveUnaryCodec[ObsId]

  private[formats] implicit lazy val observeCommandCodec: Codec[Observe] = deriveCodec[Observe]
  private[formats] implicit lazy val setupCommandCodec: Codec[Setup] = deriveCodec[Setup]
  private[formats] implicit lazy val waitCommandCodec: Codec[Wait] = deriveCodec[Wait]
  implicit lazy val sequenceCommandCodec: Codec[SequenceCommand] = deriveCodec[SequenceCommand]
  implicit lazy val controlCommandCodec: Codec[ControlCommand] = deriveCodec[ControlCommand]
  implicit lazy val commandCodec: Codec[Command] = deriveCodec[Command]
  implicit lazy val sequenceCodec: Codec[Sequence] = deriveCodec[Sequence]

  // ************************ CommandResponse Codecs ********************

  implicit lazy val resultCodec: Codec[Result] = deriveCodec[Result]
  implicit lazy val validateCommandResponseCodec: Codec[ValidateCommandResponse] = deriveCodec[ValidateCommandResponse]
  implicit lazy val validateResponseCodec: Codec[ValidateResponse] = deriveCodec[ValidateResponse]
  implicit lazy val oneWayCommandResponseCodec: Codec[OnewayResponse] = deriveCodec[OnewayResponse]
  implicit lazy val submitCommandResponseCodec: Codec[SubmitResponse] = deriveCodec[SubmitResponse]
  implicit lazy val queryCommandResponseCodec: Codec[QueryResponse] = deriveCodec[QueryResponse]
  implicit lazy val matchingCommandResponseCodec: Codec[MatchingResponse] = deriveCodec[MatchingResponse]
  private[formats] implicit lazy val acceptedCodec: Codec[Accepted] = deriveCodec[Accepted]
  private[formats] implicit lazy val startedCodec: Codec[Started] = deriveCodec[Started]
  private[formats] implicit lazy val completedWithResultCodec: Codec[CompletedWithResult] = deriveCodec[CompletedWithResult]
  private[formats] implicit lazy val completedCodec: Codec[Completed] = deriveCodec[Completed]
  private[formats] implicit lazy val invalidCodec: Codec[Invalid] = deriveCodec[Invalid]
  private[formats] implicit lazy val errorCodec: Codec[Error] = deriveCodec[Error]
  private[formats] implicit lazy val cancelledCodec: Codec[Cancelled] = deriveCodec[Cancelled]
  private[formats] implicit lazy val lockedCodec: Codec[Locked] = deriveCodec[Locked]
  private[formats] implicit lazy val commandNotAvailableCodec: Codec[CommandNotAvailable] = deriveCodec[CommandNotAvailable]
  implicit lazy val commandResponseCodec: Codec[CommandResponse] = deriveCodec[CommandResponse]

  // ************************ CommandIssue Codecs ********************

  private[formats] implicit lazy val missingKeyIssueCodec: Codec[MissingKeyIssue] = deriveCodec[MissingKeyIssue]
  private[formats] implicit lazy val wrongPrefixIssueCodec: Codec[WrongPrefixIssue] = deriveCodec[WrongPrefixIssue]
  private[formats] implicit lazy val wrongParameterTypeIssueCodec: Codec[WrongParameterTypeIssue] =
    deriveCodec[WrongParameterTypeIssue]
  private[formats] implicit lazy val wrongUnitsIssueCodec: Codec[WrongUnitsIssue] = deriveCodec[WrongUnitsIssue]
  private[formats] implicit lazy val wrongNumberOfParametersIssueCodec: Codec[WrongNumberOfParametersIssue] =
    deriveCodec[WrongNumberOfParametersIssue]
  private[formats] implicit lazy val assemblyBusyIssueCodec: Codec[AssemblyBusyIssue] = deriveCodec[AssemblyBusyIssue]
  private[formats] implicit lazy val unresolvedLocationsIssueCodec: Codec[UnresolvedLocationsIssue] =
    deriveCodec[UnresolvedLocationsIssue]
  private[formats] implicit lazy val parameterValueOutOfRangeIssueCodec: Codec[ParameterValueOutOfRangeIssue] =
    deriveCodec[ParameterValueOutOfRangeIssue]
  private[formats] implicit lazy val wrongInternalStateIssueCodec: Codec[WrongInternalStateIssue] =
    deriveCodec[WrongInternalStateIssue]
  private[formats] implicit lazy val unsupportedCommandInStateIssueCodec: Codec[UnsupportedCommandInStateIssue] =
    deriveCodec[UnsupportedCommandInStateIssue]
  private[formats] implicit lazy val unsupportedCommandIssueCodec: Codec[UnsupportedCommandIssue] =
    deriveCodec[UnsupportedCommandIssue]
  private[formats] implicit lazy val requiredServiceUnavailableIssueCodec: Codec[RequiredServiceUnavailableIssue] =
    deriveCodec[RequiredServiceUnavailableIssue]
  private[formats] implicit lazy val requiredHCDUnavailableIssueCodec: Codec[RequiredHCDUnavailableIssue] =
    deriveCodec[RequiredHCDUnavailableIssue]
  private[formats] implicit lazy val requiredAssemblyUnavailableIssueCodec: Codec[RequiredAssemblyUnavailableIssue] =
    deriveCodec[RequiredAssemblyUnavailableIssue]
  private[formats] implicit lazy val requiredSequencerUnavailableIssueCodec: Codec[RequiredSequencerUnavailableIssue] =
    deriveCodec[RequiredSequencerUnavailableIssue]
  private[formats] implicit lazy val otherIssueCodec: Codec[OtherIssue] = deriveCodec[OtherIssue]
  implicit lazy val commandIssueCodec: Codec[CommandIssue] = deriveCodec[CommandIssue]

  // ************************ StateVariable Codecs ********************

  implicit lazy val stateNameCodec: Codec[StateName] = deriveCodec[StateName]
  implicit lazy val demandStateCodec: Codec[DemandState] = deriveCodec[DemandState]
  implicit lazy val currentStateCodec: Codec[CurrentState] = deriveCodec[CurrentState]
  implicit lazy val stateVariableCodec: Codec[StateVariable] = deriveCodec[StateVariable]

  // ************************ Subsystem Codecs ********************
  implicit lazy val subSystemCodec: Codec[Subsystem] = CodecHelpers.enumCodec[Subsystem]
}

case class Timestamp(seconds: Long, nanos: Long) {
  def toInstant: Instant = Instant.ofEpochSecond(seconds, nanos)
}

object Timestamp {
  def fromInstant(instant: Instant): Timestamp = Timestamp(instant.getEpochSecond, instant.getNano)
}

case class ParamCore[T](
    keyName: String,
    values: ArrayS[T],
    units: Units
)

object ParamCore {
  def toParam[T](map: Map[String, ParamCore[T]]): Parameter[T] = {
    val (keyType, param) = map.head
    Parameter(param.keyName, KeyType.withNameInsensitive(keyType).asInstanceOf[KeyType[T]], param.values, param.units)
  }
  def fromParam[T](param: Parameter[T]): Map[String, ParamCore[T]] = {
    Map(param.keyType.entryName -> ParamCore(param.keyName, param.items, param.units))
  }
}
