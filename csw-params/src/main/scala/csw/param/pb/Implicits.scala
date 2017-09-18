package csw.param.pb

import java.time.Instant

import com.google.protobuf.timestamp.Timestamp
import com.trueaccord.scalapb.TypeMapper

object Implicits {
  implicit val typeMapper: TypeMapper[Timestamp, Instant] =
    TypeMapper[Timestamp, Instant] { x =>
      Instant.ofEpochSecond(x.seconds)
    } { x =>
      Timestamp().withSeconds(x.getEpochSecond)
    }
}
