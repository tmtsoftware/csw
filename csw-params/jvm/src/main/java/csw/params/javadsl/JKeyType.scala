package csw.params.javadsl

import csw.params.core.formats.JsonSupport
import csw.params.core.generics.{KeyType, SimpleKeyType}
import csw.params.core.models.{ArrayData, MatrixData}
import play.api.libs.json.Format

import scala.reflect.ClassTag

/**
 * SimpleKeyType with a name for java Keys. Holds instances of primitives such as char, int, String etc.
 */
sealed class JSimpleKeyType[S: Format: ClassTag, T](implicit conversion: T ⇒ S) extends SimpleKeyType[S]

/**
 * A java KeyType that holds array
 */
sealed class JArrayKeyType[S: Format: ClassTag, T](
    implicit conversion: T ⇒ S
) extends JSimpleKeyType[ArrayData[S], ArrayData[T]]

/**
 * A java KeyType that holds matrix
 */
sealed class JMatrixKeyType[S: Format: ClassTag, T](
    implicit conversion: T ⇒ S
) extends JSimpleKeyType[MatrixData[S], MatrixData[T]]

object JSimpleKeyType {
  import JsonSupport._
  case object BooleanKey extends JSimpleKeyType[java.lang.Boolean, Boolean]
  case object CharKey    extends JSimpleKeyType[java.lang.Character, Char]

  case object ByteKey   extends JSimpleKeyType[java.lang.Byte, Byte]
  case object ShortKey  extends JSimpleKeyType[java.lang.Short, Short]
  case object LongKey   extends JSimpleKeyType[java.lang.Long, Long]
  case object IntKey    extends JSimpleKeyType[java.lang.Integer, Int]
  case object FloatKey  extends JSimpleKeyType[java.lang.Float, Float]
  case object DoubleKey extends JSimpleKeyType[java.lang.Double, Double]

  case object ByteArrayKey   extends JArrayKeyType[java.lang.Byte, Byte]
  case object ShortArrayKey  extends JArrayKeyType[java.lang.Short, Short]
  case object LongArrayKey   extends JArrayKeyType[java.lang.Long, Long]
  case object IntArrayKey    extends JArrayKeyType[java.lang.Integer, Int]
  case object FloatArrayKey  extends JArrayKeyType[java.lang.Float, Float]
  case object DoubleArrayKey extends JArrayKeyType[java.lang.Double, Double]

  case object ByteMatrixKey   extends JMatrixKeyType[java.lang.Byte, Byte]
  case object ShortMatrixKey  extends JMatrixKeyType[java.lang.Short, Short]
  case object LongMatrixKey   extends JMatrixKeyType[java.lang.Long, Long]
  case object IntMatrixKey    extends JMatrixKeyType[java.lang.Integer, Int]
  case object FloatMatrixKey  extends JMatrixKeyType[java.lang.Float, Float]
  case object DoubleMatrixKey extends JMatrixKeyType[java.lang.Double, Double]
}

/////////////////////////////////////

/**
 * KeyTypes defined for consumption in Java code
 */
object JKeyType {
  val ChoiceKey    = KeyType.ChoiceKey
  val RaDecKey     = KeyType.RaDecKey
  val StringKey    = KeyType.StringKey
  val StructKey    = KeyType.StructKey
  val TimestampKey = KeyType.TimestampKey

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
