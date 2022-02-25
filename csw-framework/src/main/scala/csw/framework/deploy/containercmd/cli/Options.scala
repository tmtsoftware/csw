package csw.framework.deploy.containercmd.cli

import java.nio.file.Path

/**
 * Command line options
 *
 * @param local               if true, start using the config file corresponding to the value inputFilePath or else
 *                            use ConfigService to fetch the file
 * @param inputFilePath       config file path
 */
private[containercmd] case class Options(local: Boolean = false, inputFilePath: Option[Path] = None)
