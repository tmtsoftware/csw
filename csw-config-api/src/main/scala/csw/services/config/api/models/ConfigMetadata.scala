package csw.services.config.api.models

import java.nio.file.Path

/**
 * Holds metadata information about config server
 */
case class ConfigMetadata(repoPath: String, annexPath: String, annexMinFileSize: String, maxConfigFileSize: String)
