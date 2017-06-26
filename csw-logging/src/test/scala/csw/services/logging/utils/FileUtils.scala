package csw.services.logging.utils

import java.io.File

import com.persist.JsonOps
import com.persist.JsonOps.JsonObject

import scala.collection.mutable

object FileUtils {

  def deleteRecursively(file: File): Unit = {
    // just to be safe, don't delete anything that is not in /tmp/
    val p = file.getPath
    if (!p.startsWith("/tmp/"))
      throw new RuntimeException(s"Refusing to delete $file since not in /tmp/")

    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }

  def read(filePath: String): mutable.Buffer[JsonObject] = {
    val fileSource = scala.io.Source.fromFile(filePath)
    val logBuffer  = mutable.Buffer.empty[JsonObject]

    fileSource.mkString.lines.foreach { line ⇒
      logBuffer += JsonOps.Json(line).asInstanceOf[JsonObject]
    }
    fileSource.close()
    logBuffer
  }
}
