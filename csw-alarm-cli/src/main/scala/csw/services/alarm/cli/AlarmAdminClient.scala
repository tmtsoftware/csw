package csw.services.alarm.cli
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.cli.args.Options
import csw.services.alarm.cli.extensions.RichFutureExt.RichFuture
import csw.services.alarm.cli.utils.ConfigUtils
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
    } transformWithSideEffect printLine

  def severity(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.setSeverity(options.alarmKey, options.severity))
      .transformWithSideEffect(printLine)

  def acknowledge(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.acknowledge(options.alarmKey))
      .transformWithSideEffect(printLine)

  def activate(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.activate(options.alarmKey))
      .transformWithSideEffect(printLine)

  def deactivate(options: Options): Future[Unit] =
    alarmServiceF
      .flatMap(_.deactivate(options.alarmKey))
      .transformWithSideEffect(printLine)
}
