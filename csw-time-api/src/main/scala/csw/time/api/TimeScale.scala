package csw.time.api

sealed trait TimeScale

object TimeScales {
  case object UTCScale extends TimeScale
  case object TAIScale extends TimeScale

  val jUTCScale: TimeScale = UTCScale
  val jTAIScale: TimeScale = TAIScale
}
