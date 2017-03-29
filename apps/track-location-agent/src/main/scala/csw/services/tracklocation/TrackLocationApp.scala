package csw.services.tracklocation

import akka.Done
import akka.actor.Terminated
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import csw.services.tracklocation.models.{Command, Options}
import csw.services.tracklocation.utils.CmdLineArgsParser

import async.Async._
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

  def start(args: Array[String]): Unit = {
    CmdLineArgsParser.parser.parse(args, Options()) match {
      case Some(options) =>
        try {
          val command = Command.parse(options)
          println(s"commandText: ${command.commandText}, command: $command")
          trackLocation = new TrackLocation(options.names, command, actorRuntime, locationService)
          trackLocation.run().recover {
            case NonFatal(ex) =>
              ex.printStackTrace()
              Await.ready(locationService.shutdown(), 10.seconds)
          }
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            Await.ready(locationService.shutdown(), 10.seconds)
            System.exit(1)
        }
      case None          =>
        Await.ready(locationService.shutdown(), 10.seconds)
        System.exit(1)
    }
  }

  def shutdown(): Future[Done] = locationService.shutdown()
}

object TrackLocationApp extends App {
  new TrackLocationApp(new ActorRuntime(Settings().withPort(2553))).start(args)
}
