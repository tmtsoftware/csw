package csw.services.config.server.commons

import java.util.regex.Pattern

import org.tmatesoft.svn.core.{SVNDirEntry, SVNNodeKind}

object SVNDirEntryExt {

  implicit class RichSvnDirEntry(val entry: SVNDirEntry) extends AnyVal {
    def isFile: Boolean                                = entry.getKind == SVNNodeKind.FILE
    def isNotDefault(defaultFileName: String): Boolean = !entry.getName.endsWith(defaultFileName)
    def matches(mayBePattern: Option[Pattern]): Boolean = mayBePattern match {
      case Some(pattern) ⇒ pattern.asPredicate().test(entry.getRelativePath)
      case None          ⇒ true
    }
  }
}
