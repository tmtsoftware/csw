package tmt.sequencer.dsl

import tmt.sequencer.util.FutureExt.RichFuture
import tmt.services.{Command, CommandResponse, LocationService}

import scala.concurrent.ExecutionContext

class CommandService(locationService: LocationService)(implicit ec: ExecutionContext) {
  def setup(componentName: String, command: Command): CommandResponse = {
    locationService.resolve(componentName).setup(command)
  }.await

  def split(params: List[Int]): (List[Int], List[Int]) = params.partition(_ % 2 != 0)
}
