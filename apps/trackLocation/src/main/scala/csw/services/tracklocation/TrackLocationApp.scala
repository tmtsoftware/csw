package csw.services.tracklocation

import akka.actor.Terminated
import csw.services.location.common.ActorRuntime
import csw.services.tracklocation.models.{Command, Options}
import csw.services.tracklocation.utils.CmdLineArgsParser

import scala.concurrent.Future

/**
  * A utility application that starts a given external program, registers it with the location service and
  * unregisters it when the program exits.
  */
// Parse the command line options
object TrackLocationApp extends App {
  val actorRuntime = new ActorRuntime("track-location-app")

  CmdLineArgsParser.parser.parse(args, Options()) match {
    case Some(options) =>
      try {
        val command = Command.parse(options)
        println(s"commandText: ${command.commandText}, command: $command")
        val trackLocation = new TrackLocation(options.names, command, actorRuntime)
        trackLocation.run()
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          System.exit(1)
      }
    case None          => System.exit(1)
  }

  def shutdown(): Future[Terminated] = actorRuntime.actorSystem.terminate()
}


