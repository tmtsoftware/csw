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
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.collection.mutable.{WrappedArray => WArray}
import scala.reflect.ClassTag

object ParamCodecs extends CommonCodecs {
  import CborHelpers._

  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  // ************************ Base Type Codecs ********************

  //Ensure that Codec.forCaseClass is used ONLY for unary case classes see https://github.com/sirthias/borer/issues/26
  implicit lazy val choiceCodec: Codec[Choice] = Codec.forCaseClass[Choice]
  implicit lazy val raDecCodec: Codec[RaDec]   = deriveCodec[RaDec]

  implicit lazy val tagCodec: Codec[Coords.Tag]                      = deriveCodec[Coords.Tag]
  implicit lazy val icrsCodec: Codec[Coords.ICRS.type]               = deriveCodec[Coords.ICRS.type]
  implicit lazy val angleCodec: Codec[Angle]                         = deriveCodec[Angle]
  implicit lazy val properMotionCodec: Codec[ProperMotion]           = deriveCodec[ProperMotion]
  implicit lazy val eqFrameCodec: Codec[EqFrame]                     = deriveCodec[EqFrame]
  implicit lazy val solarSystemObjectCodec: Codec[SolarSystemObject] = deriveCodec[SolarSystemObject]

  implicit lazy val eqCoordCodec: Codec[EqCoord]                   = deriveCodec[EqCoord]
  implicit lazy val solarSystemCoordCodec: Codec[SolarSystemCoord] = deriveCodec[SolarSystemCoord]
  implicit lazy val minorPlanetCoordCodec: Codec[MinorPlanetCoord] = deriveCodec[MinorPlanetCoord]
  implicit lazy val cometCoordCodec: Codec[CometCoord]             = deriveCodec[CometCoord]
  implicit lazy val altAzCoordCodec: Codec[AltAzCoord]             = deriveCodec[AltAzCoord]
  implicit lazy val coordCodec: Codec[Coord]                       = deriveCodec[Coord]

  implicit lazy val tsCodec: Codec[Timestamp]    = deriveCodec[Timestamp]
  implicit lazy val instantCodec: Codec[Instant] = bimap[Timestamp, Instant](_.toInstant, Timestamp.fromInstant)

  implicit lazy val utcTimeCodec: Codec[UTCTime] = Codec.forCaseClass[UTCTime]
  implicit lazy val taiTimeCodec: Codec[TAITime] = Codec.forCaseClass[TAITime]

  // ************************ Composite Codecs ********************

  implicit def arrayDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    bimap[WArray[T], ArrayData[T]](ArrayData(_), _.data)

  implicit def matrixDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    bimap[WArray[WArray[T]], MatrixData[T]](MatrixData(_), _.data)

  // ************************ Enum Codecs ********************

  implicit lazy val unitsCodec: Codec[Units]                   = enumCodec[Units]
  implicit lazy val keyTypeCodecExistential: Codec[KeyType[_]] = enumCodec[KeyType[_]]
  implicit def keyTypeCodec[T]: Codec[KeyType[T]]              = keyTypeCodecExistential.asInstanceOf[Codec[KeyType[T]]]

  // ************************ Parameter Codecs ********************

  //Do not replace these with bimap, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val javaByteArrayEnc: Encoder[Array[JByte]] = Encoder.forByteArray.contramap(_.map(x ⇒ x: Byte))
  implicit lazy val javaByteArrayDec: Decoder[Array[JByte]] = Decoder.forByteArray.map(_.map(x ⇒ x: JByte))

  implicit lazy val bytesEnc: Encoder[Array[Byte]] = Encoder { (w, value) ⇒
    val enc = if (w.target == Cbor) Encoder.forByteArray else Encoder.forArray[Byte]
    enc.write(w, value)
  }

  implicit lazy val bytesDec: Decoder[Array[Byte]] = Decoder { r ⇒
    val dec = if (r.target == Cbor) Decoder.forByteArray else Decoder.forArray[Byte]
    dec.read(r)
  }

  implicit def waCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[WArray[T]]       = bimap[Array[T], WArray[T]](x => x: WArray[T], _.array)
  implicit def paramCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[Parameter[T]] = deriveCodec[Parameter[T]]

  implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    val encoder: Encoder[Parameter[Any]] = value.keyType.paramEncoder.asInstanceOf[Encoder[Parameter[Any]]]
    encoder.write(w, value.asInstanceOf[Parameter[Any]])
  }

  implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    r.tryReadMapHeader(4) || r.tryReadMapStart() || r.tryReadArrayHeader(4) || r.tryReadArrayStart()
    r.tryReadString("keyName")
    val keyName = r.readString()
    r.tryReadString("keyType")
    val keyType = KeyType.withNameInsensitive(r.readString())
    r.tryReadString("items")
    val wa = keyType.waDecoder.read(r)
    r.tryReadString("units")
    val units = unitsCodec.decoder.read(r)
    if (r.target != Cbor) {
      r.tryReadBreak()
    }
    Parameter(keyName, keyType.asInstanceOf[KeyType[Any]], wa.asInstanceOf[WArray[Any]], units)
  }

  // ************************ Struct Codecs ********************

  implicit lazy val structCodec: Codec[Struct] = deriveCodec[Struct]

  // ************************ Event Codecs ********************

  //Codec.forCaseClass does not work for id due to https://github.com/sirthias/borer/issues/23
  implicit lazy val idCodec: Codec[Id]               = bimap[String, Id](Id(_), _.id)
  implicit lazy val eventNameCodec: Codec[EventName] = Codec.forCaseClass[EventName]

  implicit lazy val seCodec: Codec[SystemEvent]  = deriveCodec[SystemEvent]
  implicit lazy val oeCodec: Codec[ObserveEvent] = deriveCodec[ObserveEvent]
  implicit lazy val eventCodec: Codec[Event]     = deriveCodec[Event]

  // ************************ Command Codecs ********************

  implicit lazy val commandNameCodec: Codec[CommandName] = Codec.forCaseClass[CommandName]
  implicit lazy val obsIdCodec: Codec[ObsId]             = Codec.forCaseClass[ObsId]

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
  implicit lazy val subSystemCodec: Codec[Subsystem] = enumCodec[Subsystem]
}

case class Timestamp(seconds: Long, nanos: Long) {
  def toInstant: Instant = Instant.ofEpochSecond(seconds, nanos)

}

object Timestamp {
  def fromInstant(instant: Instant): Timestamp = Timestamp(instant.getEpochSecond, instant.getNano)
}
