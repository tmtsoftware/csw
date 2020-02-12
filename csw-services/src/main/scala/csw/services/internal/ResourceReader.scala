package csw.services.internal

import java.io.{File, PrintWriter}

import scala.io.Source

object ResourceReader {

  def copyToTmp(fileName: String, suffix: String = ".tmp", transform: String => String = identity): File = {
    val source = Source.fromResource(fileName)
    try {
      val tempFile = File.createTempFile(fileName, suffix)
      val writer   = new PrintWriter(tempFile)
      try {
        tempFile.deleteOnExit()
        source.getLines().foreach(line => writer.println(transform(line)))
      }
      finally writer.close()
      tempFile
    }
    finally source.close()
  }

}
