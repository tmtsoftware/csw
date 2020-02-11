package csw.services

import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.services.Command.Start
import csw.services.utils.Environment

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends CommandApp[Command] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = "0.1.0-SNAPSHOT"
  override def progName: String   = "CswServices"

  override def run(command: Command, remainingArgs: RemainingArgs): Unit = {
    command match {
      case Start(true, _, _, _, _, iface)                  => start(config = true, event = true, alarm = true, database = true, iface)
      case Start(_, config, event, alarm, database, iface) => start(config, event, alarm, database, iface)
    }
  }

  def start(config: Boolean, event: Boolean, alarm: Boolean, database: Boolean, maybeInterface: Option[String]): Unit = {
    val settings = Settings(maybeInterface)
    Environment.setup(settings)
    val agent = new LocationAgent(settings)
    val redis = new Redis(settings)

    val locationServer = Future(LocationServer.start(settings.clusterPort))
    if (event) redis.startEvent()
    if (alarm) redis.startAlarm()
    Future(agent.startSentinel(event, alarm))
    if (database) Future(agent.startPostgres())

    Await.result(locationServer, Duration.Inf)
  }

}
