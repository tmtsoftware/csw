package csw.messages.params.pb

import java.time.Instant

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import com.trueaccord.scalapb.TypeMapper

object Implicits {
  implicit val instantMapper: TypeMapper[Timestamp, Instant] =
    TypeMapper[Timestamp, Instant] { x =>
      Instant.ofEpochSecond(x.seconds)
    } { x =>
      Timestamp().withSeconds(x.getEpochSecond)
    }

  implicit val bytesMapper: TypeMapper[ByteString, Seq[Byte]] =
    TypeMapper[ByteString, Seq[Byte]](_.toByteArray)(xs ⇒ ByteString.copyFrom(xs.toArray))

  implicit val charsMapper: TypeMapper[String, Seq[Char]] =
    TypeMapper[String, Seq[Char]](s ⇒ s)(xs ⇒ String.copyValueOf(xs.toArray))

  implicit val shortMapper: TypeMapper[Int, Short] = TypeMapper[Int, Short](x ⇒ x.toShort)(x ⇒ x)
}
