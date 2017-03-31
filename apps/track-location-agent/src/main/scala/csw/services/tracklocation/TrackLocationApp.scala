package csw.services.tracklocation

import akka.Done
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.{CswCluster, LocationServiceFactory}
import csw.services.tracklocation.models.{Command, Options}
import csw.services.tracklocation.utils.CmdLineArgsParser

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/**
  * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
  */
class TrackLocationApp(cswCluster: CswCluster) {
  import cswCluster._
  val locationService= LocationServiceFactory.withCluster(cswCluster)

  var trackLocation: TrackLocation = _

  def start(args: Array[String]): Any = {
    CmdLineArgsParser.parser.parse(args, Options()) match {
      case Some(options) =>
        try {
          val command = Command.parse(options)
          println(s"commandText: ${command.commandText}, command: $command")
          trackLocation = new TrackLocation(options.names, command, cswCluster, locationService)
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
  new TrackLocationApp(CswCluster.withSettings(Settings())).start(args)
}
