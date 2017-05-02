package csw.services.config.api.commons

import akka.util.ByteString

object BinaryUtils {
  def isBinary(byteStrings: Seq[ByteString]): Boolean = {
    val bs = byteStrings.foldLeft(ByteString.empty)((bs, d) ⇒ bs ++ d)

    val bytes: Array[Byte] = bs.toArray.take(1024)

    bytes.contains(0) || isNotText(bytes)
  }

  def isNotText(bytes: Array[Byte]): Boolean = {
    var binaryCount = 0
    bytes.foreach { b =>
      if (b < 0x07 || (b > 0x0d && b < 0x20) || b > 0x7F) binaryCount += 1
    }

    bytes.length > 0 && binaryCount * 1000 / bytes.length > 850
  }
}
