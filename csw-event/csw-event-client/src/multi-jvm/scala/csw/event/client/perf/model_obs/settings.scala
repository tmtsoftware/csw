package csw.event.client.perf.model_obs

import csw.params.core.models.Prefix
import csw.event.client.perf.model_obs.BaseSetting.{PubSetting, SubSetting}

sealed trait BaseSetting {
  def prefix: Prefix
  def totalTestMsgs: Long
  def rate: Int
  def payloadSize: Int

  val warmup: Int       = rate * 20
  val newPrefix: Prefix = Prefix(s"${prefix.prefix}-${rate}Hz")
}

object BaseSetting {
  case class PubSetting(prefix: Prefix, noOfPubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
  case class SubSetting(prefix: Prefix, noOfSubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
}

case class JvmSetting(name: String, pubSettings: List[PubSetting], subSettings: List[SubSetting])

case class ModelObservatoryTestSettings(jvmSettings: List[JvmSetting])
