/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.data.config

import csw.config.models.FileType
import csw.config.models.codecs.ConfigCodecs
import csw.contract.ResourceFetcher
import csw.contract.generator.*

object ConfigContract extends ConfigData with ConfigCodecs {
  private val models: ModelSet = ModelSet.models(
    ModelType(FileType),
    ModelType(configId),
    ModelType(configFileInfo),
    ModelType(configFileRevision),
    ModelType(configMetadata)
  )
  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("config-service/README.md"))
  val service: Service = Service(
    Contract.empty,
    Contract.empty,
    models,
    readme
  )
}
