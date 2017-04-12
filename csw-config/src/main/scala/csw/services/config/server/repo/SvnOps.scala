package csw.services.config.server.repo

import java.io.OutputStream
import java.nio.file.Path

import akka.Done
import akka.dispatch.MessageDispatcher
import csw.services.config.server.Settings
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}

import scala.concurrent.Future
import scala.util.control.NonFatal

class SvnOps(settings: Settings, dispatcher: MessageDispatcher) {

  private implicit val blockingIoDispatcher = dispatcher

  def getFile(path: Path, revision: Long, outputStream: OutputStream, onError: Throwable ⇒ Unit): Future[Done] = {
    val svn = svnHandle()

    Future {
      svn.getFile(path.toString, revision, null, outputStream)
      outputStream.flush()
      outputStream.close()
    } recover {
      case NonFatal(ex) ⇒ onError(ex)
    } map { _ ⇒
      svn.closeSession()
      Done
    }
  }

  // Gets an object for accessing the svn repository (not reusing a single instance since not thread safe)
  private def svnHandle(): SVNRepository = {
    val svn = SVNRepositoryFactory.create(settings.svnUrl)
    val authManager = BasicAuthenticationManager.newInstance(settings.`svn-user-name`, Array[Char]())
    svn.setAuthenticationManager(authManager)
    svn
  }

}
