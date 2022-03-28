/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework.components

import csw.framework.exceptions.FailureStop

case class ConfigNotAvailableException() extends FailureStop("Configuration not available. Initialization failure.")
