package csw.config.helpers

import java.nio.file.Files

object ResourceFileReader {

  def read(fileName: String): (String, String) = {
    val resourceStream = getClass.getResourceAsStream(fileName)
    try {
      import java.io.File
      val tempFile = File.createTempFile(String.valueOf(resourceStream.hashCode), ".tmp")
      tempFile.deleteOnExit()

      Files.write(tempFile.toPath, resourceStream.readAllBytes())
      (tempFile.getPath, scala.io.Source.fromFile(tempFile).mkString)
    } finally {
      resourceStream.close()
    }
  }
}
