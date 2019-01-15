package csw.location.agent.utils

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigResolveOptions}

object Utils {

  /**
   * Returns an `Option` of `Config` pointed by the file parameter, else None.
   */
  def getAppConfig(file: File): Option[Config] =
    if (file.exists()) Some(ConfigFactory.parseFileAnySyntax(file).resolve(ConfigResolveOptions.noSystem())) else None
}
