package csw.testkit.internal
import java.io.File

import akka.Done
import akka.actor.CoordinatedShutdown.{Reason, UnknownReason}
import akka.util.Timeout

import scala.concurrent.{Await, Future}

private[testkit] object TestKitUtils {

  def await[T](f: Future[T], timeout: Timeout): T = Await.result(f, timeout.duration)

  def coordShutdown(f: Reason ⇒ Future[Done], timeout: Timeout): Done = await(f.apply(UnknownReason), timeout.duration)

  /**
   * Deletes the contents of the given directory (recursively).
   * This is meant for use by tests that need to always start with an empty Svn repository.
   */
  def deleteDirectoryRecursively(dir: File): Unit = {
    // just to be safe, don't delete anything that is not in /tmp/
    val p = dir.getPath
    if (!p.startsWith("/tmp/"))
      throw new RuntimeException(s"Refusing to delete $dir since not in /tmp/")

    if (dir.isDirectory) {
      dir.list.foreach { filePath =>
        val file = new File(dir, filePath)
        if (file.isDirectory) deleteDirectoryRecursively(file)
        else file.delete()
      }
      dir.delete()
    }
  }

}
