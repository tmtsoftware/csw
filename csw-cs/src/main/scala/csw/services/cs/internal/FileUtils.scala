package csw.services.cs.internal

import java.io.{File, IOException}
import java.nio.channels.FileChannel
import java.nio.file.{Path, StandardOpenOption}
import java.nio.{ByteBuffer, MappedByteBuffer}

import akka.util.ByteString
import net.codejava.security.HashGeneratorUtils

import scala.concurrent.Promise

/**
 * File utility methods
 */
object FileUtils {

  /**
   * Returns a memory mapped byte buffer for the given path
   */
  def mmap(path: Path): MappedByteBuffer = {
    val channel = FileChannel.open(path, StandardOpenOption.READ)
    val result = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size())
    channel.close()
    result
  }

  /**
   * Verifies that the given file's content matches the SHA-1 id and sets the
   * value of the promise to the id on success.
   * @param id the SHA-1 of the file
   * @param file the file to check
   * @param status the return status is success(file), or failure(exception)
   */
  def validate(id: String, file: File, status: Promise[File]): Unit =
    if (validate(id, file)) {
      status.success(file)
    } else {
      status.failure(validateError(id, file))
    }

  /**
   * Verifies that the given file's content matches the SHA-1 id
   * @param id the SHA-1 of the file
   * @param file the file to check
   * @return true if the file is valid
   */
  def validate(id: String, file: File): Boolean =
    id == HashGeneratorUtils.generateSHA1(file)

  def validateError(id: String, file: File): IOException =
    new IOException(s"Invalid contents of file $file, does not match SHA-1 $id")

  class ByteBufferIterator(buffer: ByteBuffer, chunkSize: Int) extends Iterator[ByteString] {
    require(buffer.isReadOnly)
    require(chunkSize > 0)

    override def hasNext = buffer.hasRemaining

    override def next(): ByteString = {
      val size = chunkSize min buffer.remaining()
      val temp = buffer.slice()
      temp.limit(size)
      buffer.position(buffer.position() + size)
      ByteString(temp)
    }
  }
}
