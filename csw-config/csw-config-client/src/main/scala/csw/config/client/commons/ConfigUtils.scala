/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.client.commons

import java.nio.file.{Files, Path}

import org.apache.pekko.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.config.api.exceptions.{FileNotFound, LocalFileNotFound, UnableToParseOptions}
import csw.config.api.scaladsl.ConfigClientService

import scala.async.Async.{async, await}
import scala.concurrent.Future

class ConfigUtils(configClientService: ConfigClientService)(implicit system: ActorSystem[_]) {

  import system.executionContext

  // fetch config file either from config server or local disk
  def getConfig(isLocal: Boolean, inputFilePath: Option[Path], defaultConfig: Option[Config]): Future[Config] =
    (inputFilePath, defaultConfig) match {
      case (None, None)         => throw UnableToParseOptions
      case (None, Some(config)) => Future.successful(config)
      case (Some(inputFile), _) => getConfig(inputFile, isLocal)
    }

  def getConfig(inputFilePath: Path, isLocal: Boolean): Future[Config] =
    if (isLocal) getConfigFromLocalFile(inputFilePath)
    else getConfigFromRemoteFile(inputFilePath)

  private def getConfigFromLocalFile(inputFilePath: Path): Future[Config] =
    async {
      val config =
        if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
        else throw LocalFileNotFound(inputFilePath)
      config
    }

  private def getConfigFromRemoteFile(inputFilePath: Path): Future[Config] =
    async {
      await(configClientService.getActive(inputFilePath)) match {
        case Some(configData) => await(configData.toConfigObject)
        case None             => throw FileNotFound(inputFilePath)
      }
    }
}
