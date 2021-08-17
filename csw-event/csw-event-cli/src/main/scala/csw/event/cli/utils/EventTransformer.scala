package csw.event.cli.utils

import csw.params.events.{Event, ObserveEvent, SystemEvent}

object EventTransformer {

  def transform(event: Event, paths: List[String]): Event = {
    val transformedParamSet = PathSelector(paths).transform(event.paramSet)
    event match {
      case e: ObserveEvent => e.copy(paramSet = transformedParamSet)
      case e: SystemEvent  => e.copy(paramSet = transformedParamSet)
    }
  }
}
