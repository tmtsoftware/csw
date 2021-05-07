package csw.params.core.formats

import java.lang.{Byte => JByte}

import csw.params.commands._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Coords._
import csw.params.core.models._
import csw.params.core.states.{CurrentState, StateName, StateVariable}
import csw.params.events.{Event, EventName}
import csw.prefix.codecs.CommonCodecs
import csw.time.core.models.{TAITime, UTCTime}
import io.bullet.borer._
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

import scala.collection.mutable.{ArraySeq => ArrayS}
import scala.reflect.ClassTag

object JParamCodecs extends ParamCodecs {
  //just needed for the Java test, which should not exist
  val eqFrameCodec: Codec[EqFrame] = Codec.of[EqFrame]
}
object ParamCodecs extends ParamCodecs

/**
 * Supports (de)serialization of csw models
 */
trait ParamCodecs extends ParamCodecsBase {
  implicit def commandResponseCodec[T <: CommandResponse]: Codec[T] = commandResponseCodecValue.asInstanceOf[Codec[T]]
  implicit def coordCodec[T <: Coord]: Codec[T]                     = coordCodecValue.asInstanceOf[Codec[T]]
}

trait ParamCodecsBase extends CommonCodecs {
  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  // default Char codec uses ascii which makes it difficult at typescript side
  private lazy val charCodec: Codec[Char] = Codec
    .of[String]
    .bimap[Char](
      _.toString,
      str => if (str.length == 1) str.charAt(0) else throw new RuntimeException(s"Unable to parse $str, char was expected")
    )

  implicit lazy val charEnc: Encoder[Char]           = charCodec.encoder
  implicit lazy val charDec: Decoder[Char]           = charCodec.decoder
  implicit lazy val characterEnc: Encoder[Character] = charCodec.encoder.asInstanceOf[Encoder[Character]]
  implicit lazy val characterDec: Decoder[Character] = charCodec.decoder.asInstanceOf[Decoder[Character]]

  // ************************ Base Type Codecs ********************
  implicit lazy val choiceCodec: Codec[Choice] = deriveCodec
  implicit lazy val raDecCodec: Codec[RaDec]   = deriveCodec

  implicit lazy val tagCodec: Codec[Coords.Tag]            = deriveCodec
  implicit lazy val angleCodec: Codec[Angle]               = deriveCodec
  implicit lazy val properMotionCodec: Codec[ProperMotion] = deriveCodec

  lazy val coordCodecValue: Codec[Coord] = deriveAllCodecs

  implicit lazy val utcTimeCodec: Codec[UTCTime] = deriveCodec
  implicit lazy val taiTimeCodec: Codec[TAITime] = deriveCodec

  // ************************ Composite Codecs ********************
  implicit def arrayDataCodec[T: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    Codec.bimap[Array[T], ArrayData[T]](_.values, ArrayData.fromArray)

  implicit def matrixDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    Codec.bimap[Array[Array[T]], MatrixData[T]](_.values, MatrixData.fromArrays)

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
  implicit lazy val structCodec: Codec[Struct] = MapBasedCodecs.deriveCodec

  // ************************ Event Codecs ********************
  implicit lazy val idCodec: Codec[Id]               = deriveCodec
  implicit lazy val eventNameCodec: Codec[EventName] = deriveCodec
  implicit lazy val eventCodec: Codec[Event]         = deriveAllCodecs

  // ************************ Command Codecs ********************
  implicit lazy val commandNameCodec: Codec[CommandName]         = deriveCodec
  implicit lazy val obsIdCodec: Codec[ObsId]                     = Codec.bimap[String, ObsId](_.toString, ObsId(_))
  implicit lazy val controlCommandCodec: Codec[ControlCommand]   = deriveAllCodecs
  implicit lazy val sequenceCommandCodec: Codec[SequenceCommand] = deriveAllCodecs
  implicit lazy val sequenceCodec: Codec[Sequence]               = deriveCodec

  // ************************ CommandResponse Codecs ********************
  implicit lazy val resultCodec: Codec[Result]                         = MapBasedCodecs.deriveCodec
  protected lazy val commandResponseCodecValue: Codec[CommandResponse] = deriveAllCodecs

  // ************************ CommandIssue Codecs ********************
  implicit lazy val commandIssueCodecValue: Codec[CommandIssue] = deriveAllCodecs

  // ************************ StateVariable Codecs ********************
  implicit lazy val stateNameCodec: Codec[StateName]              = deriveCodec
  implicit lazy val currentStateCodecValue: Codec[CurrentState]   = deriveCodec
  implicit lazy val stateVariableCodecValue: Codec[StateVariable] = deriveAllCodecs
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
