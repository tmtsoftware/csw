package csw.params.core.formats

import csw.params.core.generics.{KeyType, Parameter, SimpleKeyType}
import csw.params.core.models.{Struct, Units}
import io.bullet.borer._
import io.bullet.borer.derivation.CompactMapBasedCodecs

// FlatParamCodecs are specifically designed for DMS, do not use it elsewhere
// Decoder expects that keyType is always the first member of the parameter
object FlatParamCodecs extends ParamCodecs {

  case object FlatStructKey extends SimpleKeyType[Struct] {
    override def toString: String = "StructKey"
  }

  override implicit def paramCodec[T: ArrayEnc: ArrayDec]: Codec[Parameter[T]] = {
    implicit lazy val keyCodec: Codec[KeyType[T]] = Codec.of[KeyType[_]].asInstanceOf[Codec[KeyType[T]]]
    CompactMapBasedCodecs.deriveCodec
  }

  override implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    val keyType: KeyType[_]              = if (value.keyType == KeyType.StructKey) FlatStructKey else value.keyType
    val encoder: Encoder[Parameter[Any]] = keyType.flatParamEncoder.asInstanceOf[Encoder[Parameter[Any]]]
    encoder.write(w, value.asInstanceOf[Parameter[Any]])
  }

  override implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    val isUnbounded = r.readMapOpen(4)
    val keyTypeName = r.readString("keyType").readString()
    val keyType0    = KeyType.withNameInsensitive(keyTypeName)
    val keyType     = (if (keyType0 == KeyType.StructKey) FlatStructKey else keyType0).asInstanceOf[KeyType[Any]]
    val keyName     = r.readString("keyName").readString()
    val values      = r.readString("values").read()(keyType._arraySeqCodec.decoder)
    val units       = r.readString("units").read()(Decoder[Units])
    val param       = Parameter(keyType, keyName, values, units)
    r.readMapClose(isUnbounded, param)
    param
  }
}
