package csw.services

import akka.actor.CoordinatedShutdown
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.services.cli.Command
import csw.services.cli.Command.Start
import csw.services.internal.Wiring

import scala.util.control.NonFatal

object Main extends CommandApp[Command] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: Command, remainingArgs: RemainingArgs): Unit =
    command match {
      case s: Start => run(s)
    }

  def run(start: Start): Unit = {
    val wiring = new Wiring(start)
    import wiring._
    try {
      environment.setup()
      LoggingSystemFactory.start(appName, appVersion, settings.hostName, actorSystem)
      lazyLocationService
      lazyEventProcess
      lazyAlarmProcess
      lazySentinel
      lazyDatabaseService
      lazyKeycloak
      lazyConfigService

      CoordinatedShutdown(actorSystem).addJvmShutdownHook(shutdown())
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
        exit(1)
    }
  }
}
