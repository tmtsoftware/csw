/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.network.utils.exceptions

case class NetworkInterfaceNotFound(message: String) extends Exception(message)
