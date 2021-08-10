package csw.testkit.internal
import java.io.File
import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}

private[csw] object TestKitUtils {

  def await[T](f: Future[T], timeout: Timeout): T = Await.result(f, timeout.duration)

  def shutdown(f: => Future[Done], timeout: Timeout): Done = await(f, timeout.duration)

  def addToCoordinatedShutdown(name: String, task: () => Unit)(implicit actorSystem: ActorSystem[_]): Unit =
    CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseServiceUnbind, name) { () =>
      Future {
        task()
        Done
      }(ExecutionContext.global)
    }

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
