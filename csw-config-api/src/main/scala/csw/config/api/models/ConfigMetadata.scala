package csw.config.api.models

/**
 * Holds metadata information about config server
 *
 * @param repoPath the path of the repository created on config server
 * @param annexPath the path of the repository created to store large files on config server
 * @param annexMinFileSize the minimum size of the file (lower limit) which qualifies it to be considered as large file
 *                         and store it in repository located at `annexPath`
 * @param maxConfigFileSize the maximum size of file (upper limit) which qualifies it to be considered as non-large file
 *                          and store it in repository located at `repoPath`
 */
case class ConfigMetadata(repoPath: String, annexPath: String, annexMinFileSize: String, maxConfigFileSize: String) {
  override def toString: String =
    s"""
       |Repository Path: $repoPath
       |Annex store Path: $annexPath
       |Annex file min Size: $annexMinFileSize
       |Max Config File Size: $maxConfigFileSize
     """.stripMargin
}
