package csw.apps.deployment.containercmd.cli

import java.nio.file.Path

/**
 * Command line options
 *
 * @param standalone          if true, run component(s) without a container
 * @param local               if true, start using the config file corresponding to the value inputFilePath or else
 *                            use ConfigService to fetch the file
 * @param inputFilePath       config file path
 */
case class Options(
    standalone: Boolean = false,
    local: Boolean = false,
    inputFilePath: Option[Path] = None
)
