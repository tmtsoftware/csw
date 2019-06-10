package csw.params.javadsl

import csw.params.core.formats.JsonSupport
import csw.params.core.generics.{ArrayKeyType, KeyType, MatrixKeyType, SimpleKeyType}

object JSimpleKeyType {
  import JsonSupport._

  //required for Scala compatible and efficient ByteArray codecs
  import csw.params.core.formats.CborSupport.{javaByteArrayDec, javaByteArrayEnc}

  case object BooleanKey extends SimpleKeyType[java.lang.Boolean]
  case object CharKey    extends SimpleKeyType[java.lang.Character]

  case object ByteKey   extends SimpleKeyType[java.lang.Byte]
  case object ShortKey  extends SimpleKeyType[java.lang.Short]
  case object LongKey   extends SimpleKeyType[java.lang.Long]
  case object IntKey    extends SimpleKeyType[java.lang.Integer]
  case object FloatKey  extends SimpleKeyType[java.lang.Float]
  case object DoubleKey extends SimpleKeyType[java.lang.Double]

  case object ByteArrayKey   extends ArrayKeyType[java.lang.Byte]
  case object ShortArrayKey  extends ArrayKeyType[java.lang.Short]
  case object LongArrayKey   extends ArrayKeyType[java.lang.Long]
  case object IntArrayKey    extends ArrayKeyType[java.lang.Integer]
  case object FloatArrayKey  extends ArrayKeyType[java.lang.Float]
  case object DoubleArrayKey extends ArrayKeyType[java.lang.Double]

  case object ByteMatrixKey   extends MatrixKeyType[java.lang.Byte]
  case object ShortMatrixKey  extends MatrixKeyType[java.lang.Short]
  case object LongMatrixKey   extends MatrixKeyType[java.lang.Long]
  case object IntMatrixKey    extends MatrixKeyType[java.lang.Integer]
  case object FloatMatrixKey  extends MatrixKeyType[java.lang.Float]
  case object DoubleMatrixKey extends MatrixKeyType[java.lang.Double]
}

/////////////////////////////////////

/**
 * KeyTypes defined for consumption in Java code
 */
object JKeyType {
  val ChoiceKey  = KeyType.ChoiceKey
  val RaDecKey   = KeyType.RaDecKey
  val StringKey  = KeyType.StringKey
  val StructKey  = KeyType.StructKey
  val UTCTimeKey = KeyType.UTCTimeKey
  val TAITimeKey = KeyType.TAITimeKey

  val BooleanKey = JSimpleKeyType.BooleanKey
  val CharKey    = JSimpleKeyType.CharKey

  val ByteKey   = JSimpleKeyType.ByteKey
  val ShortKey  = JSimpleKeyType.ShortKey
  val LongKey   = JSimpleKeyType.LongKey
  val IntKey    = JSimpleKeyType.IntKey
  val FloatKey  = JSimpleKeyType.FloatKey
  val DoubleKey = JSimpleKeyType.DoubleKey

  val ByteArrayKey   = JSimpleKeyType.ByteArrayKey
  val ShortArrayKey  = JSimpleKeyType.ShortArrayKey
  val LongArrayKey   = JSimpleKeyType.LongArrayKey
  val IntArrayKey    = JSimpleKeyType.IntArrayKey
  val FloatArrayKey  = JSimpleKeyType.FloatArrayKey
  val DoubleArrayKey = JSimpleKeyType.DoubleArrayKey

  val ByteMatrixKey   = JSimpleKeyType.ByteMatrixKey
  val ShortMatrixKey  = JSimpleKeyType.ShortMatrixKey
  val LongMatrixKey   = JSimpleKeyType.LongMatrixKey
  val IntMatrixKey    = JSimpleKeyType.IntMatrixKey
  val FloatMatrixKey  = JSimpleKeyType.FloatMatrixKey
  val DoubleMatrixKey = JSimpleKeyType.DoubleMatrixKey
}
