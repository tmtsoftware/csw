package csw.config.client.commons

import java.nio.file.{Files, Path}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}
import csw.config.api.exceptions.{FileNotFound, LocalFileNotFound, UnableToParseOptions}
import csw.config.api.scaladsl.ConfigClientService

import scala.async.Async.{async, await}
import scala.concurrent.Future

class ConfigUtils(configClientService: ConfigClientService)(implicit system: ActorSystem, mat: Materializer) {

  import system.dispatcher

  // fetch config file either from config server or local disk
  private[csw] def getConfig(isLocal: Boolean, inputFilePath: Option[Path], defaultConfig: Option[Config]): Future[Config] =
    (inputFilePath, defaultConfig) match {
      case (None, None)         => throw UnableToParseOptions
      case (None, Some(config)) => Future.successful(config)
      case (Some(inputFile), _) => getConfig(inputFile, isLocal)
    }

  private def getConfig(inputFilePath: Path, isLocal: Boolean) =
    if (isLocal) getConfigFromLocalFile(inputFilePath)
    else getConfigFromRemoteFile(inputFilePath)

  private def getConfigFromLocalFile(inputFilePath: Path): Future[Config] = async {
    if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
    else throw LocalFileNotFound(inputFilePath)
  }

  private def getConfigFromRemoteFile(inputFilePath: Path): Future[Config] = async {
    await(configClientService.getActive(inputFilePath)) match {
      case Some(configData) ⇒ await(configData.toConfigObject)
      case None             ⇒ throw FileNotFound(inputFilePath)
    }
  }
}
