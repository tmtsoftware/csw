package csw.messages.params.pb

import com.google.protobuf.ByteString
import com.google.protobuf.wrappers.Int32Value
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message, TypeMapper}

trait PbFormat[T] extends TypeMapper[ByteString, T]

object PbFormat {
  def apply[T](implicit x: PbFormat[T]): PbFormat[T] = x

//  implicit val longPbFormat: PbFormat[Long] = genericFormat[Int64Value, Long]
  implicit val intPbFormat: PbFormat[Int] = genericFormat[Int32Value, Int]

  implicit def genericFormat[Pb <: GeneratedMessage with Message[Pb], Csw](
      implicit mapper: TypeMapper[Pb, Csw],
      companion: GeneratedMessageCompanion[Pb]
  ): PbFormat[Csw] =
    new PbFormat[Csw] {
      override def toCustom(byteString: ByteString): Csw =
        mapper.toCustom(companion.parseFrom(byteString.toByteArray))

      override def toBase(x: Csw): ByteString = mapper.toBase(x).toByteString
    }
}
