package csw.param.pb

import com.google.protobuf.ByteString
import com.google.protobuf.wrappers.Int32Value
import com.trueaccord.scalapb._

trait PbFormat[T] {
  def read(byteString: ByteString): T
  def write(x: T): ByteString
}

object PbFormat {
  def apply[T](implicit x: PbFormat[T]): PbFormat[T] = x

  implicit val dd: PbFormat[Int] = genericFormat[Int32Value, Int]

  implicit def genericFormat[PbType <: GeneratedMessage with Message[PbType], CswType](
      implicit mapper: TypeMapper[PbType, CswType],
      companion: GeneratedMessageCompanion[PbType]
  ): PbFormat[CswType] =
    new PbFormat[CswType] {
      override def read(byteString: ByteString): CswType = mapper.toCustom(companion.parseFrom(byteString.toByteArray))
      override def write(x: CswType): ByteString         = mapper.toBase(x).toByteString
    }

//  implicit def dd[PbType <: GeneratedEnum, CswType <: EnumEntry](
//      implicit companion: GeneratedEnumCompanion[PbType],
//      enum: Enum[CswType]
//  ): PbFormat[CswType] = new PbFormat[CswType] {
//    override def read(byteString: ByteString): CswType = enum.withName(byteString.toStringUtf8)
//    override def write(x: CswType): ByteString         = companion.fromName(x.entryName).get
//  }

//  def dd[T: PbFormat]: TypeMapper[PbParameter, Parameter[T]] = ???

//  implicitly[PbFormat[Parameter[Int]]]
}
