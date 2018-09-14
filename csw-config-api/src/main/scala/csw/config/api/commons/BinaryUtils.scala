package csw.config.api.commons

import akka.util.ByteString

/**
 * Utils for binary data
 */
private[config] object BinaryUtils {

  /**
   * Identifies if the sequence of ByteString can be treated as binary
   *
   * @param byteStrings a sequence of ByteString to be tested for binary content
   * @return boolean value indicating if the the byteString represents binary content
   */
  def isBinary(byteStrings: Seq[ByteString]): Boolean = {
    val bs = byteStrings.foldLeft(ByteString.empty)((bs, d) ⇒ bs ++ d)

    val bytes: Array[Byte] = bs.toArray.take(1024)

    bytes.contains(0) || isNotText(bytes)
  }

  /**
   * Identifies if the byte array can be treated as text or not by checking if binary content is more than 15% i.e.
   * ASCII printable content is more than 85%
   *
   * @param bytes a connection to resolve to with its registered location
   * @return boolean value indicating if the bytes represents text content
   */
  def isNotText(bytes: Array[Byte]): Boolean = {
    val binaryCount = bytes.count(b ⇒ b < 0x08 || (b > 0x0d && b < 0x20) || b > 0x7F)
    bytes.length > 0 && binaryCount * 1000 / bytes.length > 150
  }
}
