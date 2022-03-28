/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.models

import java.time.Instant

/**
 * Holds information about a specific version of a config file
 *
 * @param id the ConfigId representing unique id of the file
 * @param comment the comment end user wants to provide while committing the file in config service
 * @param time capturing the time of file getting committed in config service
 */
case class ConfigFileRevision private[csw] (id: ConfigId, author: String, comment: String, time: Instant)
