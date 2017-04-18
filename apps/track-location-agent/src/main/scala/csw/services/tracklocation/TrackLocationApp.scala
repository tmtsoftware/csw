package csw.services.tracklocation

import akka.Done
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.models.Command
import csw.services.tracklocation.utils.CmdLineArgsParser

import scala.async.Async._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/**
  * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
  */
class TrackLocationApp(cswCluster: CswCluster) {
  import cswCluster._
  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  def start(args: Array[String]): Future[Done] = {

    val resultF = async {
      CmdLineArgsParser.parse(args) match {
        case Some(options) =>
          val command = Command.parse(options)
          println(s"commandText: ${command.commandText}, command: $command")
          val trackLocation = new TrackLocation(options.names, command, cswCluster, locationService)
          await(trackLocation.run())
        case None          => Done
      }
    } recover {
      case NonFatal(ex) =>
        ex.printStackTrace()
        Done
    }

    async {
      await(resultF)
      await(shutdown())
    }

  }

  def shutdown(): Future[Done] = locationService.shutdown()
}

object TrackLocationApp extends App {
  Await.result(new TrackLocationApp(CswCluster.withSettings(ClusterSettings())).start(args), Duration.Inf)
}
