/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.models

/**
 * @param configFilePath path of configuration file which is provided to container cmd app to start specified components from config
 * @param configFileLocation indicator to fetch config file either from local machine or from Configuration service
 */
private[framework] case class ContainerBootstrapInfo(
    configFilePath: String,
    configFileLocation: ConfigFileLocation
)
