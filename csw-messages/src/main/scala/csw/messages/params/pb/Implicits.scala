package csw.messages.params.pb

import java.time.Instant

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import com.trueaccord.scalapb.TypeMapper

/**
 * Type mappers for implicit conversions of data types that are not directly supported by Protobuf.
 */
object Implicits {

  /**
   * Implicit conversion of Instant(provided by Java and supported by csw) to Timestamp(supported by Protobuf)
   */
  implicit val instantMapper: TypeMapper[Timestamp, Instant] =
    TypeMapper[Timestamp, Instant] { x =>
      Instant.ofEpochSecond(x.seconds)
    } { x =>
      Timestamp().withSeconds(x.getEpochSecond)
    }

  /**
   * Implicit conversion of Seq[Byte](supported by csw) to ByteString(supported by Protobuf)
   */
  implicit val bytesMapper: TypeMapper[ByteString, Seq[Byte]] =
    TypeMapper[ByteString, Seq[Byte]](_.toByteArray)(xs ⇒ ByteString.copyFrom(xs.toArray))

  /**
   * Implicit conversion of Seq[Char](supported by csw) to String(supported by Protobuf)
   */
  implicit val charsMapper: TypeMapper[String, Seq[Char]] =
    TypeMapper[String, Seq[Char]](s ⇒ s)(xs ⇒ String.copyValueOf(xs.toArray))

  /**
   * Implicit conversion of Short(supported by csw) to Int(Protobuf doesn't support Short, hence promoted to Int)
   */
  implicit val shortMapper: TypeMapper[Int, Short] = TypeMapper[Int, Short](x ⇒ x.toShort)(x ⇒ x)
}
