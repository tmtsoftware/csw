package csw.services.alarm.cli
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.cli.args.CommandLineArgs
import csw.services.alarm.cli.utils.ConfigUtils
import csw.services.alarm.cli.wiring.ActorRuntime
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.location.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.util.{Failure, Success}

class AlarmAdminClient(
    actorRuntime: ActorRuntime,
    locationService: LocationService,
    configUtils: ConfigUtils,
    printLine: Any ⇒ Unit
) {
  import actorRuntime._

  private[alarm] val alarmServiceF: Future[AlarmAdminService] = new AlarmServiceFactory().makeAdminApi(locationService)

  def init(args: CommandLineArgs): Future[Unit] =
    async {
      val config       = await(configUtils.getConfig(args.isLocal, args.filePath, None))
      val alarmService = await(alarmServiceF)
      await(alarmService.initAlarms(config, args.reset))
    }.transform {
      case s @ Success(_)  ⇒ printLine("[SUCCESS] Alarm store successfully initialized."); s
      case f @ Failure(ex) ⇒ printLine(s"[FAILURE] Failed to initialize alarm store with error: [${ex.getMessage}]"); f
    }

  def severity(args: CommandLineArgs): Future[Unit] = {
    alarmServiceF
      .flatMap(_.setSeverity(args.alarmKey, args.severity))
      .transform {
        case s @ Success(_) ⇒
          printLine(s"[SUCCESS] Severity for alarm [${args.alarmKey.value}] is successfully set to [${args.severity.name}]."); s
        case f @ Failure(ex) ⇒
          printLine(s"[FAILURE] Failed to set severity for alarm [${args.alarmKey.value}] with error: [${ex.getMessage}]"); f
      }
  }

  def acknowledge(args: CommandLineArgs): Future[Unit] =
    alarmServiceF
      .flatMap(_.acknowledge(args.alarmKey))
      .transform {
        case s @ Success(_) ⇒ printLine(s"[SUCCESS] Alarm [${args.alarmKey.value}] is successfully acknowledged."); s
        case f @ Failure(ex) ⇒
          printLine(s"[FAILURE] Failed to acknowledge alarm [${args.alarmKey.value}] with error: [${ex.getMessage}]"); f
      }

}
