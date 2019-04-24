package csw.event.client.pb

import csw.params.core.formats.MiscJsonFormats
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.time.core.models.{TAITime, UTCTime}
import csw_protobuf.parameter._
import play.api.libs.json.Format
import scalapb.TypeMapper

import scala.reflect.ClassTag

object TypeMapperFactory extends MiscJsonFormats {

  def make(keyType: KeyType[_]): TypeMapper[PbParameter, Parameter[_]] = keyType match {
    case KeyType.ChoiceKey       ⇒ typeMapper[Choice](ChoiceItems.apply)
    case KeyType.RaDecKey        ⇒ typeMapper[RaDec](RaDecItems.apply)
    case KeyType.StringKey       ⇒ typeMapper[String](StringItems.apply)
    case KeyType.StructKey       ⇒ typeMapper[Struct](StructItems.apply)
    case KeyType.UTCTimeKey      ⇒ typeMapper[UTCTime](UTCTimeItems.apply)
    case KeyType.TAITimeKey      ⇒ typeMapper[TAITime](TAITimeItems.apply)
    case KeyType.BooleanKey      ⇒ typeMapper[Boolean](BooleanItems.apply)
    case KeyType.CharKey         ⇒ typeMapper[Char](CharItems.apply)
    case KeyType.ByteKey         ⇒ typeMapper[Byte](ByteItems.apply)
    case KeyType.ShortKey        ⇒ typeMapper[Short](ShortItems.apply)
    case KeyType.LongKey         ⇒ typeMapper[Long](LongItems.apply)
    case KeyType.IntKey          ⇒ typeMapper[Int](IntItems.apply)
    case KeyType.FloatKey        ⇒ typeMapper[Float](FloatItems.apply)
    case KeyType.DoubleKey       ⇒ typeMapper[Double](DoubleItems.apply)
    case KeyType.ByteArrayKey    ⇒ typeMapper[ArrayData[Byte]](ByteArrayItems.apply)
    case KeyType.ShortArrayKey   ⇒ typeMapper[ArrayData[Short]](ShortArrayItems.apply)
    case KeyType.LongArrayKey    ⇒ typeMapper[ArrayData[Long]](LongArrayItems.apply)
    case KeyType.IntArrayKey     ⇒ typeMapper[ArrayData[Int]](IntArrayItems.apply)
    case KeyType.FloatArrayKey   ⇒ typeMapper[ArrayData[Float]](FloatArrayItems.apply)
    case KeyType.DoubleArrayKey  ⇒ typeMapper[ArrayData[Double]](DoubleArrayItems.apply)
    case KeyType.ByteMatrixKey   ⇒ typeMapper[MatrixData[Byte]](ByteMatrixItems.apply)
    case KeyType.ShortMatrixKey  ⇒ typeMapper[MatrixData[Short]](ShortMatrixItems.apply)
    case KeyType.LongMatrixKey   ⇒ typeMapper[MatrixData[Long]](LongMatrixItems.apply)
    case KeyType.IntMatrixKey    ⇒ typeMapper[MatrixData[Int]](IntMatrixItems.apply)
    case KeyType.FloatMatrixKey  ⇒ typeMapper[MatrixData[Float]](FloatMatrixItems.apply)
    case KeyType.DoubleMatrixKey ⇒ typeMapper[MatrixData[Double]](DoubleMatrixItems.apply)
    case _                       ⇒ throw new RuntimeException("Invalid key type")
  }

  private def typeMapper[T: ClassTag: Format](itemsFactory: ItemsFactory[T]): TypeMapper[PbParameter, Parameter[_]] = {
    implicit val dd: ItemsFactory[T] = itemsFactory
    TypeMapperSupport.parameterTypeMapper[T].asInstanceOf[TypeMapper[PbParameter, Parameter[_]]]
  }
}
