package csw.services.tracklocation

import akka.Done
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import csw.services.tracklocation.models.{Command, Options}
import csw.services.tracklocation.utils.CmdLineArgsParser

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/**
  * A utility application that starts a given external program, registers it with the location service and
  * unregisters it when the program exits.
  */
class TrackLocationApp(actorRuntime: ActorRuntime) {
  import actorRuntime._
  val locationService= LocationServiceFactory.make(actorRuntime)
  var trackLocation: TrackLocation = _

  def start(args: Array[String]): Any = {
    CmdLineArgsParser.parser.parse(args, Options()) match {
      case Some(options) =>
        try {
          val command = Command.parse(options)
          println(s"commandText: ${command.commandText}, command: $command")
          trackLocation = new TrackLocation(options.names, command, actorRuntime, locationService)
          trackLocation.run().recover {
            case NonFatal(ex) =>
              ex.printStackTrace()
              shutdown()
          }
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            shutdown()
            System.exit(1)
        }
      case None          =>
        shutdown()
        System.exit(1)
    }
  }

  def shutdown(): Future[Done] = Await.ready(locationService.shutdown(), 10.seconds)
}

object TrackLocationApp extends App {
  new TrackLocationApp(ActorRuntime.withSettings(Settings())).start(args)
}
