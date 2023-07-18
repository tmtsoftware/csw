/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit.internal
import java.io.File

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.util.Timeout

import scala.concurrent.{Await, Future}

private[csw] object TestKitUtils {

  def await[T](f: Future[T], timeout: Timeout): T = Await.result(f, timeout.duration)

  def shutdown(f: => Future[Done], timeout: Timeout): Done = await(f, timeout.duration)

  def terminateHttpServerBinding(binding: ServerBinding, timeout: Timeout): Http.HttpTerminated =
    await(binding.terminate(timeout.duration), timeout)

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
