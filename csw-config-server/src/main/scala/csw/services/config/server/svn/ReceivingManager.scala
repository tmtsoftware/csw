package csw.services.config.server.svn

import java.util.regex.Pattern

import csw.services.config.api.models.FileType
import csw.services.config.server.Settings
import csw.services.config.server.commons.SVNDirEntryExt.RichSvnDirEntry
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver

class ReceivingManager(settings: Settings, fileType: Option[FileType], compiledPattern: Option[Pattern]) {

  private var entries: List[SVNDirEntry] = List.empty

  private def receiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
    if (entry.isFile && entry.isNotActiveFile(settings.`active-config-suffix`)) {
      entry.stripAnnexSuffix(settings.`sha1-suffix`)
      if (entry.matches(compiledPattern)) {
        entries = entry :: entries
      }
    }
  }

  private def normalFileReceiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
    if (entry.isFile && entry.isNotActiveFile(settings.`active-config-suffix`) && entry
          .isNormal(settings.`sha1-suffix`) && entry.matches(compiledPattern)) {
      entries = entry :: entries
    }
  }

  private def annexFileReceiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
    if (entry.isFile && entry.isNotActiveFile(settings.`active-config-suffix`) && entry
          .isAnnex(settings.`sha1-suffix`)) {
      entry.stripAnnexSuffix(settings.`sha1-suffix`)
      if (entry.matches(compiledPattern)) {
        entries = entry :: entries
      }
    }
  }

  def getReceiver: ISvnObjectReceiver[SVNDirEntry] =
    fileType match {
      case Some(FileType.Annex)  ⇒ annexFileReceiver
      case Some(FileType.Normal) ⇒ normalFileReceiver
      case _                     ⇒ receiver
    }

  def getReceivedEntries: List[SVNDirEntry] = entries
}
