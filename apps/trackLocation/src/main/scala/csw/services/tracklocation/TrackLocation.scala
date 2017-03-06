package csw.services.tracklocation

import java.io.File

import akka.actor.Terminated
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory, ConfigResolveOptions}
import csw.services.location.common.ActorRuntime
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, RegistrationResult, TcpRegistration}
import csw.services.tracklocation.models.Options
import csw.services.tracklocation.utils.{CmdLineArgsParser, OptionsHandler}
import csw.services.location.scaladsl.LocationServiceFactory

import scala.sys.process._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * A utility application that starts a given external program, registers it with the location service and
  * unregisters it when the program exits.
  */
object TrackLocation extends App {

  val runtime = new ActorRuntime("track-location-app")
  implicit val timeout = Timeout(10.seconds)

  // Parse the command line options
  CmdLineArgsParser.parser.parse(args, Options()) match {
    case Some(options) =>
      try {
        run(options)
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          System.exit(1)
      }
    case None => System.exit(1)
  }

  // Report error and exit
  private def error(msg: String): Unit = {
    println(msg)
    System.exit(1)
  }

  // Gets the application config from the file, if it exists, or from the config service, if it exists there.
  // If neither exist, an error is reported.
  private def getAppConfig(file: File): Option[Config] = {
    if (file.exists()) {
      Some(ConfigFactory.parseFileAnySyntax(file).resolve(ConfigResolveOptions.noSystem()))
    }
    else {
      None
    }
  }

  // Run the application
  private def run(options: Options): Unit = {
    if (options.names.isEmpty) error("Please specify one or more application names, separated by commas")

    //. Get the app config file, if given
    val appConfig: Option[Config] = options.appConfigFile.flatMap(getAppConfig)
    val optHandler: OptionsHandler = OptionsHandler(options, appConfig)

    // Gets the String value of an option from the command line or the app's config file, or None if not found
    // Use the value of the --port option, or use a random, free port
    val port = optHandler.portOpt("port", options.port)

    // Replace %port in the command
    val command = optHandler.stringOpt("command", options.command)
      .get
      .replace("%port", port.toString)

    val defaultDelay = 1000
    startApp(options.names, command, port, options.delay.getOrElse(defaultDelay), options.noExit)
  }

  // Starts the command and registers it with the given name on the given port
  private def startApp(names: List[String], command: String, port: Int, delay: Int, noExit: Boolean): Unit = {

    implicit val dispatcher = runtime.actorSystem.dispatcher
    val locationService = LocationServiceFactory.make(runtime)

    def registerNames: Future[List[RegistrationResult]] = Future.sequence(names.map{ name =>
      val componentId = ComponentId(name, ComponentType.Service)
      val connection = TcpConnection(componentId)
      locationService.register(TcpRegistration(connection, port))
    })

    // Insert a delay before registering with the location service to give the app a chance to start
    val f = for {
      _ <- Future { Thread.sleep(delay) }
      reg <- registerNames
    } yield reg

    // Run the command and wait for it to exit
    val exitCode = command.!

    println(s"$command exited with exit code $exitCode")

    // Unregister from the location service and exit
    val registration = Await.result(f, timeout.duration)
    registration.foreach(_.unregister())

    if (!noExit) System.exit(exitCode)
  }

  def shutdown(): Future[Terminated] = runtime.actorSystem.terminate()
}
