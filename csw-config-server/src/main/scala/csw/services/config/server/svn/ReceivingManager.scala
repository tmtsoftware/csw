package csw.services.config.server.svn

import java.util.regex.Pattern

import csw.services.config.server.Settings
import csw.services.config.server.commons.SVNDirEntryExt.RichSvnDirEntry
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver

class ReceivingManager(settings: Settings, compiledPattern: Option[Pattern], fileType: Option[String]) {

  private var entries: List[SVNDirEntry] = List.empty

  private def defaultReceiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
    if (entry.isFile && entry.isNotDefault(settings.`default-suffix`)) {
      entry.stripAnnexSuffix(settings.`sha1-suffix`)
      if (entry.matches(compiledPattern)) {
        entries = entry :: entries
      }
    }
  }

  private def normalFileReceiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
    if (entry.isFile && entry.isNotDefault(settings.`default-suffix`) && entry
          .isNormal(settings.`sha1-suffix`) && entry.matches(compiledPattern)) {
      entries = entry :: entries
    }
  }

  private def annexFileReceiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
    if (entry.isFile && entry.isNotDefault(settings.`default-suffix`) && entry.isAnnex(settings.`sha1-suffix`)) {
      entry.stripAnnexSuffix(settings.`sha1-suffix`)
      if (entry.matches(compiledPattern)) {
        entries = entry :: entries
      }
    }
  }

  def getReceiver: ISvnObjectReceiver[SVNDirEntry] =
    fileType match {
      case Some("annex")  ⇒ annexFileReceiver
      case Some("normal") ⇒ normalFileReceiver
      case _              ⇒ defaultReceiver
    }

  def getReceivedEntries: List[SVNDirEntry] = entries
}
