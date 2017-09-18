package csw.messages.params.generics

import java.time.Instant

import csw.messages.params.formats.JsonSupport
import csw.messages.params.models.Units.second
import csw.messages.params.models.{Units, _}
import com.trueaccord.scalapb.TypeMapper
import csw.param.formats.JsonSupport
import csw.param.models._
import csw.param.pb.ItemsFactory
import csw.units.Units
import csw.units.Units.second
import csw_params.keytype.PbKeyType
import csw_params.parameter.PbParameter
import enumeratum.{Enum, EnumEntry}
import spray.json.JsonFormat

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyType[S: JsonFormat: ClassTag: ItemsFactory] extends EnumEntry with Serializable {
  def paramFormat: JsonFormat[Parameter[S]]             = Parameter[S]
  def typeMapper: TypeMapper[PbParameter, Parameter[S]] = Parameter.typeMapper
}

sealed class SimpleKeyType[S: JsonFormat: ClassTag: ItemsFactory] extends KeyType[S] {
  def make(name: String): Key[S] = new Key[S](name, this)
}

sealed class SimpleKeyTypeWithUnits[S: JsonFormat: ClassTag: ItemsFactory](defaultUnits: Units) extends KeyType[S] {
  def make(name: String): Key[S] = new Key[S](name, this, defaultUnits)
}
sealed class ArrayKeyType[S: JsonFormat: ClassTag](implicit x: ItemsFactory[ArrayData[S]])
    extends SimpleKeyType[ArrayData[S]]
sealed class MatrixKeyType[S: JsonFormat: ClassTag](implicit x: ItemsFactory[MatrixData[S]])
    extends SimpleKeyType[MatrixData[S]]

//////////
sealed class JSimpleKeyType[S: JsonFormat: ClassTag, T: ItemsFactory]
    extends SimpleKeyType[S]()(implicitly, implicitly, ItemsFactory[T].asInstanceOf[ItemsFactory[S]])
sealed class JArrayKeyType[S: JsonFormat: ClassTag, T: ItemsFactory](implicit x: ItemsFactory[ArrayData[T]])
    extends JSimpleKeyType[ArrayData[S], ArrayData[T]]
sealed class JMatrixKeyType[S: JsonFormat: ClassTag, T: ItemsFactory](implicit x: ItemsFactory[MatrixData[T]])
    extends JSimpleKeyType[MatrixData[S], MatrixData[T]]

///////////////
object KeyType extends Enum[KeyType[_]] {

  import JsonSupport._

  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  case object ChoiceKey extends KeyType[Choice] {
    def make(name: String, choices: Choices): GChoiceKey = new GChoiceKey(name, this, choices)
    def make(name: String, firstChoice: Choice, restChoices: Choice*): GChoiceKey =
      new GChoiceKey(name, this, Choices(restChoices.toSet + firstChoice))
  }

  case object RaDecKey     extends SimpleKeyType[RaDec]
  case object StringKey    extends SimpleKeyType[String]
  case object StructKey    extends SimpleKeyType[Struct]
  case object TimestampKey extends SimpleKeyTypeWithUnits[Instant](second)

  //scala
  case object BooleanKey extends SimpleKeyType[Boolean]
  case object ByteKey    extends SimpleKeyType[Byte]
  case object CharKey    extends SimpleKeyType[Char]
  case object ShortKey   extends SimpleKeyType[Short]
  case object LongKey    extends SimpleKeyType[Long]
  case object IntKey     extends SimpleKeyType[Int]
  case object FloatKey   extends SimpleKeyType[Float]
  case object DoubleKey  extends SimpleKeyType[Double]

  case object ByteArrayKey   extends ArrayKeyType[Byte]
  case object ShortArrayKey  extends ArrayKeyType[Short]
  case object LongArrayKey   extends ArrayKeyType[Long]
  case object IntArrayKey    extends ArrayKeyType[Int]
  case object FloatArrayKey  extends ArrayKeyType[Float]
  case object DoubleArrayKey extends ArrayKeyType[Double]

  case object ByteMatrixKey   extends MatrixKeyType[Byte]
  case object ShortMatrixKey  extends MatrixKeyType[Short]
  case object LongMatrixKey   extends MatrixKeyType[Long]
  case object IntMatrixKey    extends MatrixKeyType[Int]
  case object FloatMatrixKey  extends MatrixKeyType[Float]
  case object DoubleMatrixKey extends MatrixKeyType[Double]

  //java
  case object JBooleanKey extends JSimpleKeyType[java.lang.Boolean, Boolean]
  case object JCharKey    extends JSimpleKeyType[java.lang.Character, Char]

  case object JByteKey   extends JSimpleKeyType[java.lang.Byte, Byte]
  case object JShortKey  extends JSimpleKeyType[java.lang.Short, Short]
  case object JLongKey   extends JSimpleKeyType[java.lang.Long, Long]
  case object JIntKey    extends JSimpleKeyType[java.lang.Integer, Int]
  case object JFloatKey  extends JSimpleKeyType[java.lang.Float, Float]
  case object JDoubleKey extends JSimpleKeyType[java.lang.Double, Double]

  case object JByteArrayKey   extends JArrayKeyType[java.lang.Byte, Byte]
  case object JShortArrayKey  extends JArrayKeyType[java.lang.Short, Short]
  case object JLongArrayKey   extends JArrayKeyType[java.lang.Long, Long]
  case object JIntArrayKey    extends JArrayKeyType[java.lang.Integer, Int]
  case object JFloatArrayKey  extends JArrayKeyType[java.lang.Float, Float]
  case object JDoubleArrayKey extends JArrayKeyType[java.lang.Double, Double]

  case object JByteMatrixKey   extends JMatrixKeyType[java.lang.Byte, Byte]
  case object JShortMatrixKey  extends JMatrixKeyType[java.lang.Short, Short]
  case object JLongMatrixKey   extends JMatrixKeyType[java.lang.Long, Long]
  case object JIntMatrixKey    extends JMatrixKeyType[java.lang.Integer, Int]
  case object JFloatMatrixKey  extends JMatrixKeyType[java.lang.Float, Float]
  case object JDoubleMatrixKey extends JMatrixKeyType[java.lang.Double, Double]

  implicit def format: JsonFormat[KeyType[_]] = enumFormat(this)

  implicit def format2[T]: JsonFormat[KeyType[T]] = enumFormat(this).asInstanceOf[JsonFormat[KeyType[T]]]

  implicit val typeMapper: TypeMapper[PbKeyType, KeyType[_]] =
    TypeMapper[PbKeyType, KeyType[_]](x ⇒ KeyType.withName(x.toString()))(x ⇒ PbKeyType.fromName(x.toString).get)
}

object JKeyTypes {
  val ChoiceKey    = KeyType.ChoiceKey
  val RaDecKey     = KeyType.RaDecKey
  val StringKey    = KeyType.StringKey
  val StructKey    = KeyType.StructKey
  val TimestampKey = KeyType.TimestampKey

  val BooleanKey = KeyType.JBooleanKey
  val CharKey    = KeyType.JCharKey

  val ByteKey   = KeyType.JByteKey
  val ShortKey  = KeyType.JShortKey
  val LongKey   = KeyType.JLongKey
  val IntKey    = KeyType.JIntKey
  val FloatKey  = KeyType.JFloatKey
  val DoubleKey = KeyType.JDoubleKey

  val ByteArrayKey   = KeyType.JByteArrayKey
  val ShortArrayKey  = KeyType.JShortArrayKey
  val LongArrayKey   = KeyType.JLongArrayKey
  val IntArrayKey    = KeyType.JIntArrayKey
  val FloatArrayKey  = KeyType.JFloatArrayKey
  val DoubleArrayKey = KeyType.JDoubleArrayKey

  val ByteMatrixKey   = KeyType.JByteMatrixKey
  val ShortMatrixKey  = KeyType.JShortMatrixKey
  val LongMatrixKey   = KeyType.JLongMatrixKey
  val IntMatrixKey    = KeyType.JIntMatrixKey
  val FloatMatrixKey  = KeyType.JFloatMatrixKey
  val DoubleMatrixKey = KeyType.JDoubleMatrixKey
}
