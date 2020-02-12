package csw.services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.services.Command.Start
import csw.services.utils.Environment

import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, ExecutionContext, Future}

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
    val settings = Settings(maybeInterface)
    Environment.setup(settings)

    implicit val actorSystem: ActorSystem[Nothing] = ActorSystemFactory.remote(Behaviors.empty)
    implicit val ec: ExecutionContext              = actorSystem.executionContext

    val locationService = HttpLocationServiceFactory.makeLocalClient
    val agent           = new LocationAgent(settings)
    val redis           = new Redis(settings)
    val keycloak        = new AuthServer(locationService, settings)

    val locationServer = Future(LocationServer.start(settings.clusterPort))
    if (event) redis.startEvent()
    if (alarm) redis.startAlarm()
    Future(agent.startSentinel(event, alarm))
    if (database) Future(agent.startPostgres())

    // if config is true, then start auth + config
    if (config) {
      Await.result(keycloak.start, 20.minutes)
      ConfigServer.start(settings.configPort)
    }
    else if (auth) Await.result(keycloak.start, 20.minutes)

    Await.result(locationServer, Duration.Inf)
  }

}
