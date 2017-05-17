package csw.services.config.server.commons
import java.util.regex.Pattern

import csw.services.config.api.models.FileType
import org.tmatesoft.svn.core.{SVNDirEntry, SVNNodeKind}

object SVNDirEntryExt {

  /**
   * Adds extra features to SVNDirEntry
   */
  implicit class RichSvnDirEntry(val entry: SVNDirEntry) extends AnyVal {
    def isFile: Boolean                                  = entry.getKind == SVNNodeKind.FILE
    def isNotActiveFile(activeFileName: String): Boolean = !entry.getName.endsWith(activeFileName)
    def stripAnnexSuffix(annexSuffix: String): Unit =
      entry.setRelativePath(entry.getRelativePath.stripSuffix(annexSuffix))
    def matches(maybePattern: Option[Pattern]): Boolean = maybePattern match {
      case None          ⇒ true
      case Some(pattern) ⇒ pattern.matcher(entry.getRelativePath).matches()
    }
    def matchesFileType(maybeFileType: Option[FileType], annexSuffix: String): Boolean = maybeFileType match {
      case None                  ⇒ true
      case Some(FileType.Annex)  ⇒ isAnnex(annexSuffix)
      case Some(FileType.Normal) ⇒ !isAnnex(annexSuffix)
    }
    private def isAnnex(annexSuffix: String): Boolean = entry.getName.endsWith(annexSuffix)
  }
}
