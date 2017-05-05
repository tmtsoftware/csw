package csw.services.config.api.models

/**
 * Holds metadata information about config server
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
