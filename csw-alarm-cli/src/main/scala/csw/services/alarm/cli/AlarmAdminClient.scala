package csw.services.alarm.cli
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.cli.args.Options
import csw.services.alarm.cli.extensions.RichFutureExt.RichFuture
import csw.services.alarm.cli.utils.{ConfigUtils, Formatter}
import csw.services.alarm.cli.wiring.ActorRuntime
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.location.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.concurrent.Future

class AlarmAdminClient(
    actorRuntime: ActorRuntime,
    locationService: LocationService,
    configUtils: ConfigUtils,
    printLine: Any ⇒ Unit
) {
  import actorRuntime._

  private[alarm] val alarmServiceF: Future[AlarmAdminService] = new AlarmServiceFactory().makeAdminApi(locationService)

  def init(options: Options): Future[Unit] =
    async {
      val config       = await(configUtils.getConfig(options.isLocal, options.filePath, None))
      val alarmService = await(alarmServiceF)
      await(alarmService.initAlarms(config, options.reset))
    }.transformWithSideEffect(printLine)

  def severity(options: Options): Future[Unit] = async {
    val alarmService = await(alarmServiceF)
    options.severity match {
      case Some(_) ⇒ await(alarmService.setSeverity(options.alarmKey, options.severity.get).transformWithSideEffect(printLine))
      case None ⇒
        val severity = await(alarmService.getCurrentSeverity(options.alarmKey))
        printLine(Formatter.formatSeverity(severity))
    }
  }

  def acknowledge(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.acknowledge(options.alarmKey))
      .transformWithSideEffect(printLine)

  def unacknowledge(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.unacknowledge(options.alarmKey))
      .transformWithSideEffect(printLine)

  def activate(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.activate(options.alarmKey))
      .transformWithSideEffect(printLine)

  def deactivate(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.deactivate(options.alarmKey))
      .transformWithSideEffect(printLine)

  def shelve(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.shelve(options.alarmKey))
      .transformWithSideEffect(printLine)

  def unshelve(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.unshelve(options.alarmKey))
      .transformWithSideEffect(printLine)

  def reset(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.reset(options.alarmKey))
      .transformWithSideEffect(printLine)

  def list(options: Options): Future[Unit] = async {
    val adminService = await(alarmServiceF)
    val metadataSet  = await(adminService.getMetadata(options.key)).sortBy(_.name)
    printLine(Formatter.formatMetadataSet(metadataSet))
  }

  def status(options: Options): Future[Unit] = async {
    val adminService = await(alarmServiceF)
    val status       = await(adminService.getStatus(options.alarmKey))
    printLine(Formatter.formatStatus(status))
  }
}
