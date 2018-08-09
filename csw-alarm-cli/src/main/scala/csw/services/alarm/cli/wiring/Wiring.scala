package csw.services.alarm.cli.wiring
import akka.actor.ActorSystem
import csw.services.alarm.cli.{AlarmAdminClient, CommandExecutor}
import csw.services.alarm.cli.utils.ConfigUtils
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class Wiring(actorSystem: ActorSystem) {
  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._

  lazy val locationService: LocationService = LocationServiceFactory.makeRemoteHttpClient
  lazy val configUtils                      = new ConfigUtils(actorRuntime, locationService)
  lazy val alarmAdminClient                 = new AlarmAdminClient(actorRuntime, locationService, configUtils)
  lazy val commandExecutor                  = new CommandExecutor(alarmAdminClient)
}
