package csw.params.core.formats

import java.lang.{Byte => JByte}
import java.time.Instant

import com.github.ghik.silencer.silent
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

object JParamCodecs extends ParamCodecs {
  //just needed for the Java test, which should not exist
  val eqFrameCodec: Codec[EqFrame] = implicitly[Codec[EqFrame]]
}
object ParamCodecs extends ParamCodecs

/**
 * Supports (de)serialization of csw models
 */
trait ParamCodecs extends CommonCodecs {

  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  // ************************ Base Type Codecs ********************
  implicit lazy val choiceCodec: Codec[Choice] = deriveUnaryCodec
  implicit lazy val raDecCodec: Codec[RaDec]   = deriveCodec

  implicit lazy val tagCodec: Codec[Coords.Tag]            = deriveUnaryCodec
  implicit lazy val angleCodec: Codec[Angle]               = deriveUnaryCodec
  implicit lazy val properMotionCodec: Codec[ProperMotion] = deriveCodec

  implicit lazy val eqCoordCodec: Codec[EqCoord]                   = deriveCodec
  implicit lazy val solarSystemCoordCodec: Codec[SolarSystemCoord] = deriveCodec
  implicit lazy val minorPlanetCoordCodec: Codec[MinorPlanetCoord] = deriveCodec
  implicit lazy val cometCoordCodec: Codec[CometCoord]             = deriveCodec
  implicit lazy val altAzCoordCodec: Codec[AltAzCoord]             = deriveCodec
  implicit lazy val coordCodec: Codec[Coord]                       = deriveCodec

  implicit lazy val instantEnc: Encoder[Instant] = Encoder.targetSpecific(
    cbor = deriveEncoder[Timestamp].contramap(instant => Timestamp(instant.getEpochSecond, instant.getNano)),
    json = Encoder.forString.contramap(_.toString)
  )

  implicit lazy val instantDec: Decoder[Instant] = Decoder.targetSpecific(
    cbor = deriveDecoder[Timestamp].map(ts => Instant.ofEpochSecond(ts.seconds, ts.nanos)),
    json = Decoder.forString.map(Instant.parse)
  )

  implicit lazy val utcTimeCodec: Codec[UTCTime] = deriveUnaryCodec
  implicit lazy val taiTimeCodec: Codec[TAITime] = deriveUnaryCodec

  // ************************ Composite Codecs ********************
  implicit def arrayDataCodec[T: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    Codec.bimap[ArrayS[T], ArrayData[T]](_.data, ArrayData(_))

  implicit def matrixDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    Codec.bimap[ArrayS[ArrayS[T]], MatrixData[T]](_.data, MatrixData(_))(Encoder.forIterable, Decoder.forIterable)

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

  implicit def paramCoreCodec[T: ArrayEnc: ArrayDec]: Codec[ParamCore[T]] = deriveCodec

  implicit def paramCodec[T: ArrayEnc: ArrayDec]: Codec[Parameter[T]] =
    Codec.bimap[Map[String, ParamCore[T]], Parameter[T]](ParamCore.fromParam, ParamCore.toParam)

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
  implicit lazy val structCodec: Codec[Struct] = deriveCodec

  // ************************ Event Codecs ********************
  implicit lazy val idCodec: Codec[Id]                            = deriveUnaryCodec
  implicit lazy val eventNameCodec: Codec[EventName]              = deriveUnaryCodec
  private[formats] implicit lazy val seCodec: Codec[SystemEvent]  = deriveCodec
  private[formats] implicit lazy val oeCodec: Codec[ObserveEvent] = deriveCodec
  implicit lazy val eventCodec: Codec[Event]                      = deriveCodec

  // ************************ Command Codecs ********************
  implicit lazy val commandNameCodec: Codec[CommandName] = deriveUnaryCodec
  implicit lazy val obsIdCodec: Codec[ObsId]             = deriveUnaryCodec

  private[formats] implicit lazy val observeCommandCodec: Codec[Observe] = deriveCodec
  private[formats] implicit lazy val setupCommandCodec: Codec[Setup]     = deriveCodec
  private[formats] implicit lazy val waitCommandCodec: Codec[Wait]       = deriveCodec
  implicit lazy val sequenceCommandCodec: Codec[SequenceCommand]         = deriveCodec
  implicit lazy val controlCommandCodec: Codec[ControlCommand]           = deriveCodec
  implicit lazy val commandCodec: Codec[Command]                         = deriveCodec
  implicit lazy val sequenceCodec: Codec[Sequence]                       = deriveUnaryCodec

  // ************************ CommandResponse Codecs ********************
  implicit lazy val resultCodec: Codec[Result] = deriveCodec

  implicit def commandResponseCodec[T <: CommandResponse]: Codec[T] = commandResponseCodecValue.asInstanceOf[Codec[T]]
  lazy val commandResponseCodecValue: Codec[CommandResponse] = {
    @silent implicit lazy val acceptedCodec: Codec[Accepted]   = deriveCodec
    @silent implicit lazy val startedCodec: Codec[Started]     = deriveCodec
    @silent implicit lazy val completedCodec: Codec[Completed] = deriveCodec
    @silent implicit lazy val invalidCodec: Codec[Invalid]     = deriveCodec
    @silent implicit lazy val errorCodec: Codec[Error]         = deriveCodec
    @silent implicit lazy val cancelledCodec: Codec[Cancelled] = deriveCodec
    @silent implicit lazy val lockedCodec: Codec[Locked]       = deriveCodec
    deriveCodec
  }

  // ************************ CommandIssue Codecs ********************
  implicit def commandIssueCodec[T <: CommandIssue]: Codec[T] = commandIssueCodecValue.asInstanceOf[Codec[T]]
  lazy val commandIssueCodecValue: Codec[CommandIssue] = {
    @silent implicit lazy val idNotAvailableIssue: Codec[IdNotAvailableIssue]                                  = deriveCodec
    @silent implicit lazy val missingKeyIssueCodec: Codec[MissingKeyIssue]                                     = deriveCodec
    @silent implicit lazy val wrongPrefixIssueCodec: Codec[WrongPrefixIssue]                                   = deriveCodec
    @silent implicit lazy val wrongParameterTypeIssueCodec: Codec[WrongParameterTypeIssue]                     = deriveCodec
    @silent implicit lazy val wrongUnitsIssueCodec: Codec[WrongUnitsIssue]                                     = deriveCodec
    @silent implicit lazy val wrongNumberOfParametersIssueCodec: Codec[WrongNumberOfParametersIssue]           = deriveCodec
    @silent implicit lazy val assemblyBusyIssueCodec: Codec[AssemblyBusyIssue]                                 = deriveCodec
    @silent implicit lazy val unresolvedLocationsIssueCodec: Codec[UnresolvedLocationsIssue]                   = deriveCodec
    @silent implicit lazy val parameterValueOutOfRangeIssueCodec: Codec[ParameterValueOutOfRangeIssue]         = deriveCodec
    @silent implicit lazy val wrongInternalStateIssueCodec: Codec[WrongInternalStateIssue]                     = deriveCodec
    @silent implicit lazy val unsupportedCommandInStateIssueCodec: Codec[UnsupportedCommandInStateIssue]       = deriveCodec
    @silent implicit lazy val unsupportedCommandIssueCodec: Codec[UnsupportedCommandIssue]                     = deriveCodec
    @silent implicit lazy val requiredServiceUnavailableIssueCodec: Codec[RequiredServiceUnavailableIssue]     = deriveCodec
    @silent implicit lazy val requiredHCDUnavailableIssueCodec: Codec[RequiredHCDUnavailableIssue]             = deriveCodec
    @silent implicit lazy val requiredAssemblyUnavailableIssueCodec: Codec[RequiredAssemblyUnavailableIssue]   = deriveCodec
    @silent implicit lazy val requiredSequencerUnavailableIssueCodec: Codec[RequiredSequencerUnavailableIssue] = deriveCodec
    @silent implicit lazy val otherIssueCodec: Codec[OtherIssue]                                               = deriveCodec
    deriveCodec
  }

  // ************************ StateVariable Codecs ********************
  implicit lazy val stateNameCodec: Codec[StateName]         = deriveUnaryCodec
  implicit lazy val demandStateCodec: Codec[DemandState]     = deriveCodec
  implicit lazy val currentStateCodec: Codec[CurrentState]   = deriveCodec
  implicit lazy val stateVariableCodec: Codec[StateVariable] = deriveCodec
}

case class Timestamp(seconds: Long, nanos: Long)

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
