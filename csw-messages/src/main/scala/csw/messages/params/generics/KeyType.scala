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

object KeyType extends Enum[KeyType[_]] {

  import JsonSupport._

  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  case object ChoiceKey extends KeyType[Choice] {
    def make(name: String, choices: Choices): GChoiceKey = new GChoiceKey(name, this, choices)
    def make(name: String, firstChoice: Choice, restChoices: Choice*): GChoiceKey =
      new GChoiceKey(name, this, Choices(restChoices.toSet + firstChoice))
  }

  case object RaDecKey  extends SimpleKeyType[RaDec]
  case object StringKey extends SimpleKeyType[String]
  case object StructKey extends SimpleKeyType[Struct]

  //scala
  case object BooleanKey   extends SimpleKeyType[Boolean]
  case object ByteKey      extends SimpleKeyType[Byte]
  case object CharKey      extends SimpleKeyType[Char]
  case object ShortKey     extends SimpleKeyType[Short]
  case object LongKey      extends SimpleKeyType[Long]
  case object IntKey       extends SimpleKeyType[Int]
  case object FloatKey     extends SimpleKeyType[Float]
  case object DoubleKey    extends SimpleKeyType[Double]
  case object TimestampKey extends SimpleKeyTypeWithUnits[Instant](second)

  case object ByteArrayKey   extends SimpleKeyType[ArrayData[Byte]]
  case object ShortArrayKey  extends SimpleKeyType[ArrayData[Short]]
  case object LongArrayKey   extends SimpleKeyType[ArrayData[Long]]
  case object IntArrayKey    extends SimpleKeyType[ArrayData[Int]]
  case object FloatArrayKey  extends SimpleKeyType[ArrayData[Float]]
  case object DoubleArrayKey extends SimpleKeyType[ArrayData[Double]]

  case object ByteMatrixKey   extends SimpleKeyType[MatrixData[Byte]]
  case object ShortMatrixKey  extends SimpleKeyType[MatrixData[Short]]
  case object LongMatrixKey   extends SimpleKeyType[MatrixData[Long]]
  case object IntMatrixKey    extends SimpleKeyType[MatrixData[Int]]
  case object FloatMatrixKey  extends SimpleKeyType[MatrixData[Float]]
  case object DoubleMatrixKey extends SimpleKeyType[MatrixData[Double]]

  //java
  case object JBooleanKey   extends SimpleKeyType[java.lang.Boolean]
  case object JCharKey      extends SimpleKeyType[java.lang.Character]
  case object JByteKey      extends SimpleKeyType[java.lang.Byte]
  case object JShortKey     extends SimpleKeyType[java.lang.Short]
  case object JLongKey      extends SimpleKeyType[java.lang.Long]
  case object JIntKey       extends SimpleKeyType[java.lang.Integer]
  case object JFloatKey     extends SimpleKeyType[java.lang.Float]
  case object JDoubleKey    extends SimpleKeyType[java.lang.Double]
  case object JTimestampKey extends SimpleKeyType[java.time.Instant]

  case object JByteArrayKey   extends SimpleKeyType[ArrayData[java.lang.Byte]]
  case object JShortArrayKey  extends SimpleKeyType[ArrayData[java.lang.Short]]
  case object JLongArrayKey   extends SimpleKeyType[ArrayData[java.lang.Long]]
  case object JIntArrayKey    extends SimpleKeyType[ArrayData[java.lang.Integer]]
  case object JFloatArrayKey  extends SimpleKeyType[ArrayData[java.lang.Float]]
  case object JDoubleArrayKey extends SimpleKeyType[ArrayData[java.lang.Double]]

  case object JByteMatrixKey   extends SimpleKeyType[MatrixData[java.lang.Byte]]
  case object JShortMatrixKey  extends SimpleKeyType[MatrixData[java.lang.Short]]
  case object JLongMatrixKey   extends SimpleKeyType[MatrixData[java.lang.Long]]
  case object JIntMatrixKey    extends SimpleKeyType[MatrixData[java.lang.Integer]]
  case object JFloatMatrixKey  extends SimpleKeyType[MatrixData[java.lang.Float]]
  case object JDoubleMatrixKey extends SimpleKeyType[MatrixData[java.lang.Double]]

  implicit def format: JsonFormat[KeyType[_]] = enumFormat(this)

  implicit def format2[T]: JsonFormat[KeyType[T]] = enumFormat(this).asInstanceOf[JsonFormat[KeyType[T]]]

  implicit val typeMapper: TypeMapper[PbKeyType, KeyType[_]] =
    TypeMapper[PbKeyType, KeyType[_]](x ⇒ KeyType.withName(x.toString()))(x ⇒ PbKeyType.fromName(x.toString).get)
}

object JKeyTypes {
  val ChoiceKey = KeyType.ChoiceKey
  val RaDecKey  = KeyType.RaDecKey
  val StringKey = KeyType.StringKey
  val StructKey = KeyType.StructKey

  val BooleanKey   = KeyType.JBooleanKey
  val CharKey      = KeyType.JCharKey
  val ByteKey      = KeyType.JByteKey
  val ShortKey     = KeyType.JShortKey
  val LongKey      = KeyType.JLongKey
  val IntKey       = KeyType.JIntKey
  val FloatKey     = KeyType.JFloatKey
  val DoubleKey    = KeyType.JDoubleKey
  val TimestampKey = KeyType.JTimestampKey

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
