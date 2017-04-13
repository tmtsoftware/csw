package csw.services.csclient.utils

import java.io.File
import java.nio.file.Path

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import csw.services.config.api.models.ConfigData

import scala.concurrent.Future

object PathUtils {

  /**
    * Initialize with the contents of the file from given path.
    *
    * @param path      the data source
    * @param chunkSize the block or chunk size to use when streaming the data
    */
  def fromPath(path: Path, chunkSize: Int = 4096): ConfigData = ConfigData.fromSource(FileIO.fromPath(path, chunkSize))


  /**
    * Writes config data to a provided file and returns output file.
    */
  def writeToPath(configData: ConfigData, path: Path)(implicit mat: Materializer): Future[File] = {
    val file = new File(path.toString)
    import mat.executionContext
    configData
      .source
      .runWith(FileIO.toPath(file.toPath))
      .map(_ ⇒ file)
  }

}
