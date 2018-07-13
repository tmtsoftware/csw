package tmt.shared.dsl

import akka.stream.KillSwitch
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.messages.commands.{CommandIssue, CommandResponse, ControlCommand, ValidationResponse}
import csw.messages.location._
import csw.services.command.scaladsl.CommandService
import csw.services.location.scaladsl.LocationService
import tmt.shared.util.FutureExt.RichFuture

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class CswServicesDsl(locationService: LocationService)(implicit system: ActorSystem[_]) {
  implicit val ec: ExecutionContext = system.executionContext

  private var components: Map[String, AkkaLocation] = Map.empty //TODO: is there a concurrency problem of map ?

  def track(connectionName: String): KillSwitch = locationService.subscribe(Connection.from(connectionName), trackingCallback)

  def submit(componentName: String, controlCommand: ControlCommand, timeOut: Timeout): CommandResponse = //TODO: create dsl for Timeout and accept as parameter
    components
      .get(componentName)
      .fold[CommandResponse] {
        //TODO: decide the type of CommandIssue
        ValidationResponse.Invalid(controlCommand.runId, CommandIssue.OtherIssue(s"Unavailable component $componentName"))
      } { location =>
        new CommandService(location)
          .submit(controlCommand)(timeOut)
          .recover { case NonFatal(ex) => ValidationResponse.Invalid(controlCommand.runId, CommandIssue.OtherIssue(ex.getMessage)) }
          .await(timeOut.duration)
      }

  def split(params: List[Int]): (List[Int], List[Int]) = params.partition(_ % 2 != 0)

  private def trackingCallback(trakingEvent: TrackingEvent): Unit = trakingEvent match {
    case LocationUpdated(location: AkkaLocation) => components = components.updated(location.connection.name, location)
    case LocationRemoved(connection)             => components = components - connection.name
    case _                                       => // Do nothing
  }
}
