package csw.apps.deployment.hostconfig.cli

import java.nio.file.Path

/**
 * Command line options
 *
 * @param local               if true, get the host configuration file from local machine located at provided hostConfigPath
 *                            else, fetch the host configuration file from configuration service
 * @param hostConfigPath      host configuration file path
 */
case class Options(
    local: Boolean = false,
    hostConfigPath: Option[Path] = None
)
