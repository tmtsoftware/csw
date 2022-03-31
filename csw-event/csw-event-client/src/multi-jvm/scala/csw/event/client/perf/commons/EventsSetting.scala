/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.commons

case class EventsSetting(totalTestMsgs: Long, payloadSize: Int, warmup: Int, rate: Int)
