/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.model_obs

import csw.event.client.perf.model_obs.BaseSetting.{PubSetting, SubSetting}
import csw.prefix.models.Prefix

sealed trait BaseSetting {
  def prefix: Prefix
  def totalTestMsgs: Long
  def rate: Int
  def payloadSize: Int

  val warmup: Int       = rate * 30
  val newPrefix: Prefix = Prefix(s"${prefix}_${rate}Hz")
}

object BaseSetting {
  case class PubSetting(prefix: Prefix, noOfPubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
  case class SubSetting(prefix: Prefix, noOfSubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
}

case class JvmSetting(name: String, pubSettings: List[PubSetting], subSettings: List[SubSetting])

case class ModelObservatoryTestSettings(jvmSettings: List[JvmSetting])
