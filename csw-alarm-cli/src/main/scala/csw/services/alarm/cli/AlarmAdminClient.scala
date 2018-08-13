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

  private val alarmServiceF: Future[AlarmAdminService] = new AlarmServiceFactory().makeAdminApi(locationService)

  def init(args: CommandLineArgs): Future[Unit] = async {
    val config       = await(configUtils.getConfig(args.isLocal, args.filePath, None))
    val alarmService = await(alarmServiceF)
    val initResultF  = alarmService.initAlarms(config, args.reset)
    initResultF.onComplete {
      case Success(_)  ⇒ printLine("[SUCCESS] Alarms successfully initialized.")
      case Failure(ex) ⇒ printLine(s"[FAILURE] Failed to initialize alarm store with error: [${ex.getCause.getMessage}]")
    }
    await(initResultF)
  }

  def severity(args: CommandLineArgs): Future[Unit] = async {
    val alarmService = await(alarmServiceF)
    val key          = args.alarmKey
    val severity     = args.severity
    val setResultF   = alarmService.setSeverity(key, severity)
    setResultF.onComplete {
      case Success(_) ⇒ printLine(s"[SUCCESS] Severity for alarm [${key.value}] is successfully set to [${severity.name}].")
      case Failure(ex) ⇒
        printLine(s"[FAILURE] Failed to set severity for alarm [${key.value}] with error: [${ex.getCause.getMessage}]")
    }
    await(setResultF)
  }
}
