package csw.services.config.api.models

/**
 * Holds metadata information about config server
 */
case class ConfigMetadata(repoPath: String, annexPath: String, annexMinFileSize: String, maxConfigFileSize: String)
