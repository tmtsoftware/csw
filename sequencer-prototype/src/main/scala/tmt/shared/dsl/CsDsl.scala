package tmt.shared.dsl

import tmt.shared.util.FutureExt.RichFuture
import tmt.shared.services.{Command, CommandResponse, LocationService}

import scala.concurrent.ExecutionContext

class CsDsl(locationService: LocationService)(implicit ec: ExecutionContext) {
  def setup(componentName: String, command: Command): CommandResponse = {
    locationService.resolve(componentName).setup(command)
  }.await

  def split(params: List[Int]): (List[Int], List[Int]) = params.partition(_ % 2 != 0)
}
