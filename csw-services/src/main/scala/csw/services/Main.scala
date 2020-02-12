package csw.services

import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.services.cli.Command
import csw.services.cli.Command.Start
import csw.services.internal.Wiring

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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
    environment.setup()

    val locationServer = Future(LocationServer.start(settings.clusterPort))
    if (event) redis.startEvent()
    if (alarm) redis.startAlarm()
    Future(locationAgent.startSentinel(event, alarm))
    if (database) Future(locationAgent.startPostgres())

    // if config is true, then start auth + config
    if (config) {
      keycloak.start()
      ConfigServer.start(settings.configPort)
    }
    else if (auth) keycloak.start()

    Await.result(locationServer, Duration.Inf)
  }

}
