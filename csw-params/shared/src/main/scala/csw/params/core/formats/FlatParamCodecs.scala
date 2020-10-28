package csw.params.core.formats

import csw.params.core.generics.KeyType.StructKey
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.{Struct, Units}
import io.bullet.borer._
import io.bullet.borer.derivation.CompactMapBasedCodecs

import scala.collection.mutable

// FlatParamCodecs are specifically designed for DMS, do not use it elsewhere
// Decoder expects that keyType is always the first member of the parameter
object FlatParamCodecs extends ParamCodecs {

  override implicit def paramCodec[T: ArrayEnc: ArrayDec]: Codec[Parameter[T]] = {
    implicit lazy val keyCodec: Codec[KeyType[T]] = Codec.of[KeyType[_]].asInstanceOf[Codec[KeyType[T]]]
    CompactMapBasedCodecs.deriveCodec
  }

  override implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    value.keyType match {
      case StructKey => Encoder[Parameter[Struct]].write(w, value.asInstanceOf[Parameter[Struct]])
      case _         => value.keyType.flatParamEncoder.asInstanceOf[Encoder[Parameter[Any]]].write(w, value.asInstanceOf[Parameter[Any]])
    }
  }

  override implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    val isUnbounded = r.readMapOpen(4)
    val keyTypeName = r.readString("keyType").readString()
    val kt          = KeyType.withNameInsensitive(keyTypeName)
    val keyType     = kt.asInstanceOf[KeyType[Any]]
    val keyName     = r.readString("keyName").readString()

    val valuesDecoder = kt match {
      case StructKey => Decoder[mutable.ArraySeq[Struct]].asInstanceOf[Decoder[mutable.ArraySeq[Any]]]
      case _         => keyType.arraySeqDecoder
    }

    val values = r.readString("values").read()(valuesDecoder)
    val units  = r.readString("units").read()(Decoder[Units])
    val param  = Parameter(keyType, keyName, values, units)
    r.readMapClose(isUnbounded, param)
    param
  }
}
