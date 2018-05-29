package csw.services.event.perf.model_obs

import csw.services.event.perf.model_obs.BaseSetting.{PubSetting, SubSetting}

sealed trait BaseSetting {
  def subsystem: String
  def totalTestMsgs: Long
  def rate: Int
  def payloadSize: Int

  val warmup: Int = rate * 20
  val key         = s"$subsystem-${rate}Hz"
}

object BaseSetting {
  case class PubSetting(subsystem: String, noOfPubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
  case class SubSetting(subsystem: String, noOfSubs: Int, totalTestMsgs: Long, rate: Int, payloadSize: Int) extends BaseSetting
}

case class JvmSetting(name: String, pubSettings: List[PubSetting], subSettings: List[SubSetting])

case class ModelObservatoryTestSettings(jvmSettings: List[JvmSetting])
