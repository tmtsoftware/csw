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
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.collection.mutable.{ArraySeq => ArrayS}
import scala.reflect.ClassTag

object ParamCodecs extends ParamCodecs
trait ParamCodecs extends CommonCodecs {

  import CborHelpers._

  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  // ************************ Base Type Codecs ********************

  implicit lazy val choiceCodec: Codec[Choice] = deriveCodecForUnaryCaseClass[Choice]
  implicit lazy val raDecCodec: Codec[RaDec]   = deriveCodec[RaDec]

  implicit lazy val tagCodec: Codec[Coords.Tag]                      = deriveCodecForUnaryCaseClass[Coords.Tag]
  implicit lazy val angleCodec: Codec[Angle]                         = deriveCodecForUnaryCaseClass[Angle]
  implicit lazy val properMotionCodec: Codec[ProperMotion]           = deriveCodec[ProperMotion]
  implicit lazy val eqFrameCodec: Codec[EqFrame]                     = CborHelpers.enumCodec[EqFrame]
  implicit lazy val solarSystemObjectCodec: Codec[SolarSystemObject] = CborHelpers.enumCodec[SolarSystemObject]

  implicit lazy val eqCoordCodec: Codec[EqCoord]                   = deriveCodec[EqCoord]
  implicit lazy val solarSystemCoordCodec: Codec[SolarSystemCoord] = deriveCodec[SolarSystemCoord]
  implicit lazy val minorPlanetCoordCodec: Codec[MinorPlanetCoord] = deriveCodec[MinorPlanetCoord]
  implicit lazy val cometCoordCodec: Codec[CometCoord]             = deriveCodec[CometCoord]
  implicit lazy val altAzCoordCodec: Codec[AltAzCoord]             = deriveCodec[AltAzCoord]
  implicit lazy val coordCodec: Codec[Coord]                       = deriveCodec[Coord]

  implicit lazy val tsCodec: Codec[Timestamp] = deriveCodec[Timestamp]

  implicit lazy val instantEnc: Encoder[Instant] = CborHelpers.targetSpecificEnc(
    cborEnc = tsCodec.encoder.contramap(Timestamp.fromInstant),
    jsonEnc = Encoder.forString.contramap(_.toString)
  )

  implicit lazy val instantDec: Decoder[Instant] = CborHelpers.targetSpecificDec(
    cborDec = tsCodec.decoder.map(_.toInstant),
    jsonDec = Decoder.forString.map(Instant.parse)
  )

  implicit lazy val utcTimeCodec: Codec[UTCTime] = deriveCodecForUnaryCaseClass[UTCTime]
  implicit lazy val taiTimeCodec: Codec[TAITime] = deriveCodecForUnaryCaseClass[TAITime]

  // ************************ Composite Codecs ********************

  implicit def arrayDataCodec[T: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    bimap[ArrayS[T], ArrayData[T]](ArrayData(_), _.data)

  implicit def matrixDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    bimap[ArrayS[ArrayS[T]], MatrixData[T]](MatrixData(_), _.data)(Encoder.forIterable, Decoder.forIterable)

  // ************************ Enum Codecs ********************

  implicit lazy val unitsCodec: Codec[Units]                   = CborHelpers.enumCodec[Units]
  implicit lazy val keyTypeCodecExistential: Codec[KeyType[_]] = CborHelpers.enumCodec[KeyType[_]]

  implicit def keyTypeCodec[T]: Codec[KeyType[T]] = keyTypeCodecExistential.asInstanceOf[Codec[KeyType[T]]]

  // ************************ Parameter Codecs ********************

  //Do not replace these with bimap, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val javaByteArrayEnc: Encoder[Array[JByte]] = Encoder.forByteArray.contramap(_.map(x => x: Byte))
  implicit lazy val javaByteArrayDec: Decoder[Array[JByte]] = Decoder.forByteArray.map(_.map(x => x: JByte))

  implicit def waCodec[T: ArrayEnc: ArrayDec]: Codec[ArrayS[T]] =
    bimap[Array[T], ArrayS[T]](x => x: ArrayS[T], _.array.asInstanceOf[Array[T]])

  //Do not put the bytesEnc and bytesDec inside Codec, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val bytesEnc: Encoder[Array[Byte]] = CborHelpers.targetSpecificEnc(
    cborEnc = Encoder.forByteArray,
    jsonEnc = Encoder.forArray[Byte]
  )

  implicit lazy val bytesDec: Decoder[Array[Byte]] = CborHelpers.targetSpecificDec(
    cborDec = Decoder.forByteArray,
    jsonDec = Decoder.forArray[Byte]
  )

  implicit def paramCoreCodec[T: ArrayEnc: ArrayDec]: Codec[ParamCore[T]] = deriveCodec[ParamCore[T]]

  implicit def paramCodec[T: ArrayEnc: ArrayDec]: Codec[Parameter[T]] =
    bimap[Map[String, ParamCore[T]], Parameter[T]](ParamCore.toParam, ParamCore.fromParam)

  implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    val encoder: Encoder[Parameter[Any]] = value.keyType.paramEncoder.asInstanceOf[Encoder[Parameter[Any]]]
    encoder.write(w, value.asInstanceOf[Parameter[Any]])
  }

  implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    r.tryReadMapHeader(1) || r.tryReadMapStart() || r.tryReadArrayHeader(1) || r.tryReadArrayStart()
    val keyTypeName = r.readString()
    val keyType     = KeyType.withNameInsensitive(keyTypeName)
    val paramCore   = keyType.paramCoreDecoder.read(r)
    if (r.target != Cbor) {
      r.tryReadBreak()
    }
    ParamCore.toParam(Map(keyTypeName -> paramCore))
  }

  // ************************ Struct Codecs ********************

  implicit lazy val structCodec: Codec[Struct] = deriveCodec[Struct]

  // ************************ Event Codecs ********************

  implicit lazy val idCodec: Codec[Id]               = deriveCodecForUnaryCaseClass[Id]
  implicit lazy val eventNameCodec: Codec[EventName] = deriveCodecForUnaryCaseClass[EventName]
  implicit lazy val seCodec: Codec[SystemEvent]      = deriveCodec[SystemEvent]
  implicit lazy val oeCodec: Codec[ObserveEvent]     = deriveCodec[ObserveEvent]
  implicit lazy val eventCodec: Codec[Event]         = deriveCodec[Event]

  // ************************ Command Codecs ********************

  implicit lazy val commandNameCodec: Codec[CommandName] = deriveCodecForUnaryCaseClass[CommandName]
  implicit lazy val obsIdCodec: Codec[ObsId]             = deriveCodecForUnaryCaseClass[ObsId]

  implicit lazy val observeCommandCodec: Codec[Observe]          = deriveCodec[Observe]
  implicit lazy val setupCommandCodec: Codec[Setup]              = deriveCodec[Setup]
  implicit lazy val waitCommandCodec: Codec[Wait]                = deriveCodec[Wait]
  implicit lazy val sequenceCommandCodec: Codec[SequenceCommand] = deriveCodec[SequenceCommand]
  implicit lazy val controlCommandCodec: Codec[ControlCommand]   = deriveCodec[ControlCommand]
  implicit lazy val commandCodec: Codec[Command]                 = deriveCodec[Command]

  // ************************ CommandResponse Codecs ********************

  implicit lazy val resultCodec: Codec[Result]                                      = deriveCodec[Result]
  implicit lazy val validateCommandResponseCodec: Codec[ValidateCommandResponse]    = deriveCodec[ValidateCommandResponse]
  implicit lazy val validateResponseCodec: Codec[ValidateResponse]                  = deriveCodec[ValidateResponse]
  implicit lazy val oneWayCommandResponseCodec: Codec[OnewayResponse]               = deriveCodec[OnewayResponse]
  implicit lazy val submitCommandResponseCodec: Codec[SubmitResponse]               = deriveCodec[SubmitResponse]
  implicit lazy val queryCommandResponseCodec: Codec[QueryResponse]                 = deriveCodec[QueryResponse]
  implicit lazy val matchingCommandResponseCodec: Codec[MatchingResponse]           = deriveCodec[MatchingResponse]
  implicit lazy val acceptedCodec: Codec[Accepted]                                  = deriveCodec[Accepted]
  implicit lazy val startedCodec: Codec[Started]                                    = deriveCodec[Started]
  implicit lazy val completedWithResultCodec: Codec[CompletedWithResult]            = deriveCodec[CompletedWithResult]
  implicit lazy val completedCodec: Codec[Completed]                                = deriveCodec[Completed]
  implicit lazy val invalidCodec: Codec[Invalid]                                    = deriveCodec[Invalid]
  implicit lazy val errorCodec: Codec[Error]                                        = deriveCodec[Error]
  implicit lazy val cancelledCodec: Codec[Cancelled]                                = deriveCodec[Cancelled]
  implicit lazy val lockedCodec: Codec[Locked]                                      = deriveCodec[Locked]
  implicit lazy val commandNotAvailableCodec: Codec[CommandNotAvailable]            = deriveCodec[CommandNotAvailable]
  implicit lazy val commandResponseRemoteMsgCodec: Codec[CommandResponse.RemoteMsg] = deriveCodec[CommandResponse.RemoteMsg]
  implicit lazy val commandResponseCodec: Codec[CommandResponse]                    = deriveCodec[CommandResponse]

  // ************************ CommandIssue Codecs ********************

  implicit lazy val missingKeyIssueCodec: Codec[MissingKeyIssue]                 = deriveCodec[MissingKeyIssue]
  implicit lazy val wrongPrefixIssueCodec: Codec[WrongPrefixIssue]               = deriveCodec[WrongPrefixIssue]
  implicit lazy val wrongParameterTypeIssueCodec: Codec[WrongParameterTypeIssue] = deriveCodec[WrongParameterTypeIssue]
  implicit lazy val wrongUnitsIssueCodec: Codec[WrongUnitsIssue]                 = deriveCodec[WrongUnitsIssue]
  implicit lazy val wrongNumberOfParametersIssueCodec: Codec[WrongNumberOfParametersIssue] =
    deriveCodec[WrongNumberOfParametersIssue]
  implicit lazy val assemblyBusyIssueCodec: Codec[AssemblyBusyIssue]               = deriveCodec[AssemblyBusyIssue]
  implicit lazy val unresolvedLocationsIssueCodec: Codec[UnresolvedLocationsIssue] = deriveCodec[UnresolvedLocationsIssue]
  implicit lazy val parameterValueOutOfRangeIssueCodec: Codec[ParameterValueOutOfRangeIssue] =
    deriveCodec[ParameterValueOutOfRangeIssue]
  implicit lazy val wrongInternalStateIssueCodec: Codec[WrongInternalStateIssue] = deriveCodec[WrongInternalStateIssue]
  implicit lazy val unsupportedCommandInStateIssueCodec: Codec[UnsupportedCommandInStateIssue] =
    deriveCodec[UnsupportedCommandInStateIssue]
  implicit lazy val unsupportedCommandIssueCodec: Codec[UnsupportedCommandIssue] = deriveCodec[UnsupportedCommandIssue]
  implicit lazy val requiredServiceUnavailableIssueCodec: Codec[RequiredServiceUnavailableIssue] =
    deriveCodec[RequiredServiceUnavailableIssue]
  implicit lazy val requiredHCDUnavailableIssueCodec: Codec[RequiredHCDUnavailableIssue] =
    deriveCodec[RequiredHCDUnavailableIssue]
  implicit lazy val requiredAssemblyUnavailableIssueCodec: Codec[RequiredAssemblyUnavailableIssue] =
    deriveCodec[RequiredAssemblyUnavailableIssue]
  implicit lazy val requiredSequencerUnavailableIssueCodec: Codec[RequiredSequencerUnavailableIssue] =
    deriveCodec[RequiredSequencerUnavailableIssue]
  implicit lazy val otherIssueCodec: Codec[OtherIssue]     = deriveCodec[OtherIssue]
  implicit lazy val commandIssueCodec: Codec[CommandIssue] = deriveCodec[CommandIssue]

  // ************************ StateVariable Codecs ********************

  implicit lazy val stateNameCodec: Codec[StateName]         = deriveCodec[StateName]
  implicit lazy val demandStateCodec: Codec[DemandState]     = deriveCodec[DemandState]
  implicit lazy val currentStateCodec: Codec[CurrentState]   = deriveCodec[CurrentState]
  implicit lazy val stateVariableCodec: Codec[StateVariable] = deriveCodec[StateVariable]

  // ************************ Subsystem Codecs ********************
  implicit lazy val subSystemCodec: Codec[Subsystem] = CborHelpers.enumCodec[Subsystem]
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
