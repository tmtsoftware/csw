package tmt.shared.dsl

import akka.typed.ActorSystem
import akka.util.Timeout
import csw.messages.ccs.CommandIssue
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.location._
import csw.services.ccs.scaladsl.CommandService
import csw.services.location.scaladsl.LocationService
import tmt.shared.util.FutureExt.RichFuture

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class CsDsl(locationService: LocationService, connections: Set[Connection])(implicit system: ActorSystem[_]) {
  implicit val ec: ExecutionContext = system.executionContext

  private var components: Map[String, AkkaLocation] = Map.empty //TODO: is there a concurrency problem of map ?

  connections.foreach(locationService.subscribe(_, trackingCallback))

  def submit(componentName: String, controlCommand: ControlCommand, timeOut: Timeout): CommandResponse = //TODO: create dsl for Timeout and accept as parameter
    components
      .get(componentName)
      .fold[CommandResponse] {
        //TODO: decide the type of CommandIssue
        CommandResponse.Invalid(controlCommand.runId, CommandIssue.OtherIssue(s"Unavailable component $componentName"))
      } { location =>
        new CommandService(location)
          .submit(controlCommand)(timeOut)
          .recover { case NonFatal(ex) => CommandResponse.Invalid(controlCommand.runId, CommandIssue.OtherIssue(ex.getMessage)) }
          .await(timeOut.duration)
      }

  def split(params: List[Int]): (List[Int], List[Int]) = params.partition(_ % 2 != 0)

  private def trackingCallback(trakingEvent: TrackingEvent): Unit = trakingEvent match {
    case LocationUpdated(location: AkkaLocation) => components = components.updated(location.connection.name, location)
    case LocationRemoved(connection)             => components = components - connection.name
    case _                                       => // Do nothing
  }
}
