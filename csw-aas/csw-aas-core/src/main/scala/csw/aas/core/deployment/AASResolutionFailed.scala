/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.core.deployment

/**
 * Indicates that an attempt to resolve AAS service via location service was failed
 */
case class AASResolutionFailed(msg: String) extends Exception(msg)
