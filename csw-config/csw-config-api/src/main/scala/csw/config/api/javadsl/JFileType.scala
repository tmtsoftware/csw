package csw.config.api.javadsl

import csw.config.models.FileType

/**
 * Helper class for Java to get the handle of file types
 */
object JFileType {

  /**
   * Represents a file to be stored in annex store
   */
  val Annex: FileType = FileType.Annex

  /**
   * Represents a file to be stored in the repository normally
   */
  val Normal: FileType = FileType.Normal
}
