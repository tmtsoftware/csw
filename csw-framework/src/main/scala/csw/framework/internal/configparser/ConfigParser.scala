/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.command.client.models.framework.ComponentInfo
import csw.framework.codecs.FrameworkCodecs
import csw.framework.models.{ContainerInfo, HostBootstrapInfo}
import io.bullet.borer.{Decoder, Json}

/**
 * Parses the information represented in configuration files into respective models
 */
private[csw] object ConfigParser extends FrameworkCodecs {
  def parseContainer(config: Config): ContainerInfo  = parse[ContainerInfo](config)
  def parseStandalone(config: Config): ComponentInfo = parse[ComponentInfo](config)
  def parseHost(config: Config): HostBootstrapInfo   = parse[HostBootstrapInfo](config)

  private def parse[T: Decoder](config: Config): T =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[T].value
}
