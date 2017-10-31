package csw.apps.deployment.commons

import java.nio.file.{Files, Path}

import com.typesafe.config.{Config, ConfigFactory}
import csw.exceptions.{FileNotFound, LocalFileNotFound, UnableToParseOptions}
import csw.framework.internal.wiring.ActorRuntime
import csw.services.config.api.scaladsl.ConfigClientService

import scala.async.Async.{async, await}
import scala.concurrent.Future

class ConfigUtils(configClientService: ConfigClientService, actorRuntime: ActorRuntime) {
  import actorRuntime._

  def getConfig(
      isLocal: Boolean,
      inputFilePath: Option[Path],
      defaultConfig: Option[Config],
      configClientService: ConfigClientService
  ): Future[Config] = {
    if (inputFilePath.isEmpty && defaultConfig.isEmpty) throw UnableToParseOptions
    if (inputFilePath.isEmpty && defaultConfig.isDefined) {
      Future.successful(defaultConfig.get)
    } else if (isLocal) {
      val config = getConfigFromLocalFile(inputFilePath.get)
      Future.successful(config)
    } else getConfigFromRemoteFile(inputFilePath.get, configClientService)
  }

  def getConfigFromLocalFile(inputFilePath: Path): Config = {
    if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
    else throw LocalFileNotFound(inputFilePath)
  }

  def getConfigFromRemoteFile(inputFilePath: Path, configClientService: ConfigClientService): Future[Config] =
    async {
      await(configClientService.getActive(inputFilePath)) match {
        case Some(configData) ⇒ await(configData.toConfigObject)
        case None             ⇒ throw FileNotFound(inputFilePath)
      }
    }
}
