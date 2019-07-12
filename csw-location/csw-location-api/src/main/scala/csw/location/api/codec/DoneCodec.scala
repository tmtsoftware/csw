package csw.location.api.codec

import akka.Done
import csw.params.core.formats.CborHelpers
import io.bullet.borer.Codec

object DoneCodec extends DoneCodec
trait DoneCodec {
  implicit lazy val doneCodec: Codec[Done] = CborHelpers.bimap[String, Done](_ => Done, _ => "done")
}
