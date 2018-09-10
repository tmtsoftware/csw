package csw.services.alarm.cli.utils

import java.nio.file.{Files, Path}

import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.api.scaladsl.LocationService
import csw.services.alarm.cli.wiring.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory

import scala.async.Async.{async, await}
import scala.concurrent.Future

class ConfigUtils(actorRuntime: ActorRuntime, locationService: LocationService) {

  import actorRuntime._
  // fetch config file either from config server or local disk
  def getConfig(isLocal: Boolean, inputFilePath: Option[Path], defaultConfig: Option[Config]): Future[Config] = {
    if (inputFilePath.isEmpty && defaultConfig.isEmpty)
      Future.failed(new RuntimeException("Could not parse command line options. See --help to know more."))

    if (inputFilePath.isEmpty && defaultConfig.isDefined) Future.successful(defaultConfig.get)
    else if (isLocal) getConfigFromLocalFile(inputFilePath.get)
    else getConfigFromRemoteFile(inputFilePath.get)
  }

  private def getConfigFromLocalFile(inputFilePath: Path): Future[Config] = {
    if (Files.exists(inputFilePath)) Future.successful(ConfigFactory.parseFile(inputFilePath.toFile))
    else Future.failed(new RuntimeException(s"File does not exist on local disk at path ${inputFilePath.toString}"))
  }

  private def getConfigFromRemoteFile(inputFilePath: Path): Future[Config] =
    async {
      val configClientService = ConfigClientFactory.clientApi(system, locationService)
      await(configClientService.getActive(inputFilePath)) match {
        case Some(configData) ⇒ await(configData.toConfigObject)
        case None             ⇒ throw new RuntimeException(s"File does not exist at path=$inputFilePath")
      }
    }
}
