package csw.services.tracklocation

import akka.Done
import akka.actor.Terminated
import akka.util.Timeout
import csw.services.location.common.ActorRuntime
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, RegistrationResult, TcpRegistration}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.models.{Command, Options}
import csw.services.tracklocation.utils.CmdLineArgsParser

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._

/**
 * A utility application that starts a given external program, registers it with the location service and
 * unregisters it when the program exits.
 */
// Parse the command line options
object TrackLocation extends App {
  val runtime = new ActorRuntime("track-location-app")
  val trackLocation = new TrackLocation(runtime)

  sys addShutdownHook {
    println("Shutdown hook reached, unregistering services.")
    trackLocation.unregisterServices()
    println(s"Exiting the application.")
  }

  CmdLineArgsParser.parser.parse(args, Options()) match {
    case Some(options) =>
      try {
          trackLocation.startApp(options.names, Command.parse(options))
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          System.exit(1)
      }
    case None => System.exit(1)
  }

  def shutdown(): Future[Terminated] = runtime.actorSystem.terminate()
}

class TrackLocation(actorRuntime: ActorRuntime) {

  implicit val timeout = Timeout(10.seconds)
  implicit val dispatcher = actorRuntime.actorSystem.dispatcher
  val locationService = LocationServiceFactory.make(actorRuntime)

  private def startApp(names: List[String], command: Command): Unit = {

    def registerNames: Future[List[RegistrationResult]] =
      Future.sequence(names.map { name =>
        val componentId = ComponentId(name, ComponentType.Service)
        val connection = TcpConnection(componentId)
        locationService.register(TcpRegistration(connection, command.port))
      })

    // Insert a delay before registering with the location service to give the app a chance to start
    val f = for {
      _ <- Future { Thread.sleep(command.delay) }
      reg <- registerNames
    } yield reg

    // Run the command and wait for it to exit
    val exitCode = command.commandText.!

    println(s"$command exited with exit code $exitCode")
    Await.result(f, timeout.duration)
    unregisterServices()

    if (!command.noExit) System.exit(exitCode)
  }

  private def unregisterServices() = {
    val x: Future[Done] = locationService.unregisterAll()
    Thread.sleep(7000)
    println(s"Services are unregistered.")
  }
}
