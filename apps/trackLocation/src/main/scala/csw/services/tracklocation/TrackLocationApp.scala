package csw.services.tracklocation

import akka.actor.Terminated
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import csw.services.tracklocation.models.{Command, Options}
import csw.services.tracklocation.utils.CmdLineArgsParser

import async.Async._
import scala.concurrent.Future

/**
  * A utility application that starts a given external program, registers it with the location service and
  * unregisters it when the program exits.
  */
class TrackLocationApp(actorRuntime: ActorRuntime) {
  import actorRuntime._

  var trackLocation: TrackLocation = _

  def start(args: Array[String]): Unit = {
    CmdLineArgsParser.parser.parse(args, Options()) match {
      case Some(options) =>
        try {
          val command = Command.parse(options)
          println(s"commandText: ${command.commandText}, command: $command")
          trackLocation = new TrackLocation(options.names, command, actorRuntime)
          trackLocation.run()
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            System.exit(1)
        }
      case None          => System.exit(1)
    }
  }

  def shutdown(): Future[Terminated] = async {
    await(actorRuntime.actorSystem.terminate())
  }
}

object TrackLocationApp extends App {
  new TrackLocationApp(new ActorRuntime(Settings("crdt").withPort(2553))).start(args)
}
