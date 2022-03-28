/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.configparser

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.alarm.api.exceptions.ConfigParseException
import csw.alarm.api.internal.ValidationResult.{Failure, Success}
import csw.alarm.api.internal.{AlarmCodecs, AlarmMetadataSet}
import io.bullet.borer.{Codec, Decoder, Json}

/**
 * Parses the information represented in configuration files into respective models
 */
private[client] object ConfigParser extends AlarmCodecs {
  val ALARMS_SCHEMA: Config = ConfigFactory.parseResources("alarms-schema.conf")

  def parseAlarmMetadataSet(config: Config): AlarmMetadataSet = parse[AlarmMetadataSet](config)

  private def parse[T: Codec](config: Config): T =
    ConfigValidator.validate(config, ALARMS_SCHEMA) match {
      case Success          => configToJsValue[T](config)
      case Failure(reasons) => throw ConfigParseException(reasons)
    }

  private def configToJsValue[T: Decoder](config: Config): T = {
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[T].value
  }
}
