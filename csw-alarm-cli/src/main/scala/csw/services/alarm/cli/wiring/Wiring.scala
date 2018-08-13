package csw.services.alarm.cli.wiring
import akka.actor.ActorSystem
import csw.services.alarm.cli.utils.ConfigUtils
import csw.services.alarm.cli.{AlarmAdminClient, CommandExecutor}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class Wiring(actorSystem: ActorSystem) {
  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._

  lazy val locationService: LocationService = LocationServiceFactory.makeRemoteHttpClient
  lazy val configUtils                      = new ConfigUtils(actorRuntime, locationService)
  lazy val printLine: Any ⇒ Unit            = printLine
  lazy val alarmAdminClient                 = new AlarmAdminClient(actorRuntime, locationService, configUtils, printLine)
  lazy val commandExecutor                  = new CommandExecutor(alarmAdminClient)
}

object Wiring {
  private[alarm] def make(_actorSystem: ActorSystem, _locationService: LocationService, _printLine: Any ⇒ Unit): Wiring =
    new Wiring(_actorSystem) {
      override lazy val locationService: LocationService = _locationService
      override lazy val printLine: Any ⇒ Unit            = _printLine
    }
}
