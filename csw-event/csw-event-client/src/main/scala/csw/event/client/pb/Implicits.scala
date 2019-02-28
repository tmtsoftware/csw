package csw.event.client.pb

import java.time.Instant

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import csw.time.core.models.{TAITime, UTCTime}
import scalapb.TypeMapper

/**
 * Type mappers for implicit conversions of data types that are not directly supported by Protobuf.
 */
object Implicits {

  /**
   * Implicit conversion of Instant(provided by Java and supported by csw) to Timestamp(supported by Protobuf)
   */
  implicit val instantMapper: TypeMapper[Timestamp, Instant] =
    TypeMapper[Timestamp, Instant] { x =>
      Instant.ofEpochSecond(x.seconds, x.nanos)
    } { x =>
      Timestamp().withSeconds(x.getEpochSecond).withNanos(x.getNano)
    }

  implicit val utcMapper: TypeMapper[Timestamp, UTCTime] =
    TypeMapper[Timestamp, UTCTime] { x =>
      UTCTime(Instant.ofEpochSecond(x.seconds, x.nanos))
    } { x =>
      Timestamp().withSeconds(x.value.getEpochSecond).withNanos(x.value.getNano)
    }

  implicit val taiMapper: TypeMapper[Timestamp, TAITime] =
    TypeMapper[Timestamp, TAITime] { x =>
      TAITime(Instant.ofEpochSecond(x.seconds, x.nanos))
    } { x =>
      Timestamp().withSeconds(x.value.getEpochSecond).withNanos(x.value.getNano)
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
