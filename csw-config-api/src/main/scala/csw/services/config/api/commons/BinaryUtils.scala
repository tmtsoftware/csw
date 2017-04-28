package csw.services.config.api.commons

import akka.util.ByteString

object BinaryUtils {
  def isBinary(byteStrings: Seq[ByteString]): Boolean = {
    val bs = byteStrings.foldLeft(ByteString.empty)((bs, d) ⇒ bs ++ d)
    !bs.toArray.take(1000).forall(ch ⇒ ch >= 0 && ch < 128)
  }
}
