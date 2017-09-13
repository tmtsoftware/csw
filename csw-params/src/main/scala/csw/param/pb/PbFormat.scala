package csw.param.pb

import com.google.protobuf.ByteString
import com.google.protobuf.wrappers._
import com.trueaccord.scalapb._
import csw_params.parameter_types.Items

import scala.reflect.ClassTag

trait PbFormat[T] extends TypeMapper[ByteString, T]

object PbFormat {
  def apply[T](implicit x: PbFormat[T]): PbFormat[T] = x

  implicit val doublePbFormat: PbFormat[Double]                         = genericFormat[DoubleValue, Double]
  implicit val floatPbFormat: PbFormat[Float]                           = genericFormat[FloatValue, Float]
  implicit val longPbFormat: PbFormat[Long]                             = genericFormat[Int64Value, Long]
  implicit val intPbFormat: PbFormat[Int]                               = genericFormat[Int32Value, Int]
  implicit val booleanPbFormat: PbFormat[Boolean]                       = genericFormat[BoolValue, Boolean]
  implicit val stringPbFormat: PbFormat[String]                         = genericFormat[StringValue, String]
  implicit val byteStringPbFormat: PbFormat[ByteString]                 = genericFormat[BytesValue, ByteString]
  implicit def arrayPbFormat[T: PbFormat: ClassTag]: PbFormat[Array[T]] = genericFormat[Items, Array[T]]

  implicit def arrayTypeMapper[T: PbFormat: ClassTag]: TypeMapper[Items, Array[T]] = new TypeMapper[Items, Array[T]] {
    override def toCustom(base: Items): Array[T] =
      Items.parseFrom(base.toByteArray).values.map(x ⇒ PbFormat[T].toCustom(x)).toArray[T]
    override def toBase(custom: Array[T]): Items = Items().withValues(custom.map(x ⇒ PbFormat[T].toBase(x)))
  }

  implicit def genericFormat[PbType <: GeneratedMessage with Message[PbType], CswType](
      implicit mapper: TypeMapper[PbType, CswType],
      companion: GeneratedMessageCompanion[PbType]
  ): PbFormat[CswType] =
    new PbFormat[CswType] {
      override def toCustom(byteString: ByteString): CswType =
        mapper.toCustom(companion.parseFrom(byteString.toByteArray))
      override def toBase(x: CswType): ByteString = mapper.toBase(x).toByteString
    }

}
