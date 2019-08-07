package csw.location.api.codec

import akka.Done
import io.bullet.borer.Codec

object DoneCodec extends DoneCodec
trait DoneCodec {
  implicit lazy val doneCodec: Codec[Done] = Codec.bimap[String, Done](_ => "done", _ => Done)
}
