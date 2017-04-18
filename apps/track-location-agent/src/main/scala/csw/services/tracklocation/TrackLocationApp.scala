package csw.services.tracklocation

import akka.Done
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.models.Command
import csw.services.tracklocation.utils.CmdLineArgsParser

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import async.Async._

/**
  * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
  */
class TrackLocationApp(cswCluster: CswCluster) {
  import cswCluster._
  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  def start(args: Array[String]): Future[Done] = async {

    try {
      CmdLineArgsParser.parse(args) match {
        case Some(options) =>
          val command = Command.parse(options)
          println(s"commandText: ${command.commandText}, command: $command")
          val trackLocation = new TrackLocation(options.names, command, cswCluster, locationService)
          trackLocation.run()
        case None          => Done
      }
    } catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
        Done
    }

    await(shutdown())
  }

  def shutdown(): Future[Done] = locationService.shutdown()
}

object TrackLocationApp extends App {
  Await.result(new TrackLocationApp(CswCluster.withSettings(ClusterSettings())).start(args), Duration.Inf)
}
