package csw.event.cli.utils

import csw.params.events.{Event, ObserveEvent, SystemEvent}

object EventTransformer {

  def transform(event: Event, paths: List[String]): Event = {
    val pathAsLists         = paths.map(_.split("/").toList)
    val transformedParamSet = PathSelector(pathAsLists).transform(event.paramSet)
    event match {
      case e: ObserveEvent => e.copy(paramSet = transformedParamSet)
      case e: SystemEvent  => e.copy(paramSet = transformedParamSet)
    }
  }

}
