package csw.param.pb

import com.google.protobuf.ByteString
import com.google.protobuf.wrappers._
import com.trueaccord.scalapb._
import csw_params.parameter_types.Items
import enumeratum.{Enum, EnumEntry}

import scala.reflect.ClassTag

trait PbFormat[T] extends TypeMapper[ByteString, T]

object PbFormat {
  def apply[T](implicit x: PbFormat[T]): PbFormat[T] = x

  implicit val longPbFormat: PbFormat[Long]                             = genericFormat[Int64Value, Long]
  implicit val intPbFormat: PbFormat[Int]                               = genericFormat[Int32Value, Int]
  implicit def arrayPbFormat[T: PbFormat: ClassTag]: PbFormat[Array[T]] = genericFormat[Items, Array[T]]

  implicit def arrayTypeMapper[T: PbFormat: ClassTag]: TypeMapper[Items, Array[T]] = new TypeMapper[Items, Array[T]] {
    override def toCustom(base: Items): Array[T] =
      Items.parseFrom(base.toByteArray).values.map(x ⇒ PbFormat[T].toCustom(x)).toArray[T]
    override def toBase(custom: Array[T]): Items = Items().withValues(custom.map(x ⇒ PbFormat[T].toBase(x)))
  }

  implicit def genericFormat[Pb <: GeneratedMessage with Message[Pb], Csw](
      implicit mapper: TypeMapper[Pb, Csw],
      companion: GeneratedMessageCompanion[Pb]
  ): PbFormat[Csw] =
    new PbFormat[Csw] {
      override def toCustom(byteString: ByteString): Csw =
        mapper.toCustom(companion.parseFrom(byteString.toByteArray))
      override def toBase(x: Csw): ByteString = mapper.toBase(x).toByteString
    }

  implicit def enumMapper[Pb <: GeneratedEnum: GeneratedEnumCompanion, Csw <: EnumEntry: Enum]: TypeMapper[Pb, Csw] = {
    val csw = implicitly[Enum[Csw]]
    val pb  = implicitly[GeneratedEnumCompanion[Pb]]
    TypeMapper[Pb, Csw](x ⇒ csw.withName(x.toString()))(x ⇒ pb.fromName(x.toString).get)
  }
}
