package csw.services

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

  override def run(command: Command, remainingArgs: RemainingArgs): Unit = {
    command match {
      case Start(true, _, _, _, _, _, iface) =>
        start(config = true, event = true, alarm = true, database = true, auth = true, iface)
      case Start(_, config, event, alarm, database, auth, iface) => start(config, event, alarm, database, auth, iface)
    }
  }

  def start(
      config: Boolean,
      event: Boolean,
      alarm: Boolean,
      database: Boolean,
      auth: Boolean,
      maybeInterface: Option[String]
  ): Unit = {
    val wiring = new Wiring(maybeInterface)
    import wiring._
    try {
      environment.setup()
      LoggingSystemFactory.start(appName, appVersion, settings.hostName, actorSystem)

      LocationServer.start(settings.clusterPort)

      if (event) ignoreException(redis.startEvent())
      if (alarm) ignoreException(redis.startAlarm())
      ignoreException(locationAgent.startSentinel(event, alarm))
      if (database) ignoreException(locationAgent.startPostgres())

      // if config is true, then start auth + config
      if (config)
        ignoreException {
          keycloak.start()
          ConfigServer.start(settings.configPort)
        }
      else if (auth) ignoreException(keycloak.start())
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
        exit(1)
    }
  }

  def ignoreException(thunk: => Unit): Unit =
    try thunk
    catch {
      case NonFatal(_) =>
    }

}
