package csw.services.alarm.cli
import akka.Done
import akka.actor.CoordinatedShutdown
import csw.services.alarm.cli.args.Options
import csw.services.alarm.cli.extensions.RichFutureExt.RichFuture
import csw.services.alarm.cli.utils.{ConfigUtils, Formatter}
import csw.services.alarm.cli.wiring.ActorRuntime
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.alarm.client.internal.AlarmServiceImpl
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

  private[alarm] val alarmServiceF: AlarmServiceImpl = new AlarmServiceFactory().makeAdminApi(locationService)

  def init(options: Options): Future[Unit] =
    async {
      val config       = await(configUtils.getConfig(options.isLocal, options.filePath, None))
      val alarmService = alarmServiceF
      await(alarmService.initAlarms(config, options.reset))
    }.transformWithSideEffect(printLine)

  def getSeverity(options: Options): Future[Unit] = async {
    val alarmService = alarmServiceF
    val severity     = await(alarmService.getAggregatedSeverity(options.key))
    printLine(Formatter.formatSeverity(options.key, severity))
  }

  def setSeverity(options: Options): Future[Unit] =
    alarmServiceF.setSeverity(options.alarmKey, options.severity.get).transformWithSideEffect(printLine)

  def subscribeSeverity(options: Options): Future[Unit] = async {
    val alarmService = alarmServiceF
    val subscription = alarmService.subscribeAggregatedSeverityCallback(
      options.key,
      severity ⇒ printLine(Formatter.formatSeverity(options.key, severity))
    )

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "unsubscribe-stream"
    )(() ⇒ subscription.unsubscribe().map(_ ⇒ Done))
  }

  def acknowledge(options: Options): Future[Unit] =
    alarmServiceF
      .acknowledge(options.alarmKey)
      .transformWithSideEffect(printLine)

  def unacknowledge(options: Options): Future[Unit] =
    alarmServiceF
      .unacknowledge(options.alarmKey)
      .transformWithSideEffect(printLine)

  def activate(options: Options): Future[Unit] =
    alarmServiceF
      .activate(options.alarmKey)
      .transformWithSideEffect(printLine)

  def deactivate(options: Options): Future[Unit] =
    alarmServiceF
      .deactivate(options.alarmKey)
      .transformWithSideEffect(printLine)

  def shelve(options: Options): Future[Unit] =
    alarmServiceF
      .shelve(options.alarmKey)
      .transformWithSideEffect(printLine)

  def unshelve(options: Options): Future[Unit] =
    alarmServiceF
      .unshelve(options.alarmKey)
      .transformWithSideEffect(printLine)

  def reset(options: Options): Future[Unit] =
    alarmServiceF
      .reset(options.alarmKey)
      .transformWithSideEffect(printLine)

  def list(options: Options): Future[Unit] = async {
    val adminService = alarmServiceF
    val metadataSet  = await(adminService.getMetadata(options.key)).sortBy(_.name)
    printLine(Formatter.formatMetadataSet(metadataSet))
  }

  def status(options: Options): Future[Unit] = async {
    val adminService = alarmServiceF
    val status       = await(adminService.getStatus(options.alarmKey))
    printLine(Formatter.formatStatus(status))
  }

  def getHealth(options: Options): Future[Unit] = async {
    val alarmService = alarmServiceF
    val health       = await(alarmService.getAggregatedHealth(options.key))
    printLine(Formatter.formatHealth(options.key, health))
  }

  def subscribeHealth(options: Options): Future[Unit] = async {
    val alarmService = alarmServiceF
    alarmService.subscribeAggregatedHealthCallback(
      options.key,
      health ⇒ printLine(Formatter.formatHealth(options.key, health))
    )
  }
}
