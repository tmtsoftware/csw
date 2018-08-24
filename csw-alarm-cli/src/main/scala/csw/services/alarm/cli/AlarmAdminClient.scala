package csw.services.alarm.cli
import akka.Done
import akka.actor.CoordinatedShutdown
import akka.stream.scaladsl.{Keep, Sink}
import csw.services.alarm.api.scaladsl.AlarmSubscription
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

  private[alarm] val alarmService: AlarmServiceImpl = new AlarmServiceFactory().makeAdminApi(locationService)

  def init(options: Options): Future[Unit] =
    async {
      val config = await(configUtils.getConfig(options.isLocal, options.filePath, None))
      await(alarmService.initAlarms(config, options.reset))
    }.transformWithSideEffect(printLine)

  def getSeverity(options: Options): Future[Unit] = async {
    val severity = await(alarmService.getAggregatedSeverity(options.key))
    printLine(Formatter.formatSeverity(options.key, severity))
  }

  def setSeverity(options: Options): Future[Unit] =
    alarmService.setSeverity(options.alarmKey, options.severity.get).transformWithSideEffect(printLine)

  def subscribeSeverity(options: Options): (AlarmSubscription, Future[Done]) = {
    val (subscription, doneF) = alarmService
      .subscribeAggregatedSeverity(options.key)
      .toMat(Sink.foreach(severity ⇒ printLine(Formatter.formatSeverity(options.key, severity))))(Keep.both)
      .run()

    unsubscribeOnCoordinatedShutdown(subscription)

    (subscription, doneF)
  }

  def acknowledge(options: Options): Future[Unit] =
    alarmService.acknowledge(options.alarmKey).transformWithSideEffect(printLine)

  def unacknowledge(options: Options): Future[Unit] =
    alarmService.unacknowledge(options.alarmKey).transformWithSideEffect(printLine)

  def activate(options: Options): Future[Unit] =
    alarmService.activate(options.alarmKey).transformWithSideEffect(printLine)

  def deactivate(options: Options): Future[Unit] =
    alarmService.deactivate(options.alarmKey).transformWithSideEffect(printLine)

  def shelve(options: Options): Future[Unit] =
    alarmService.shelve(options.alarmKey).transformWithSideEffect(printLine)

  def unshelve(options: Options): Future[Unit] =
    alarmService.unshelve(options.alarmKey).transformWithSideEffect(printLine)

  def reset(options: Options): Future[Unit] =
    alarmService.reset(options.alarmKey).transformWithSideEffect(printLine)

  def list(options: Options): Future[Unit] = async {
    val metadataSet = await(alarmService.getMetadata(options.key)).sortBy(_.name)
    printLine(Formatter.formatMetadataSet(metadataSet))
  }

  def status(options: Options): Future[Unit] = async {
    val status = await(alarmService.getStatus(options.alarmKey))
    printLine(Formatter.formatStatus(status))
  }

  def getHealth(options: Options): Future[Unit] = async {
    val health = await(alarmService.getAggregatedHealth(options.key))
    printLine(Formatter.formatHealth(options.key, health))
  }

  def subscribeHealth(options: Options): (AlarmSubscription, Future[Done]) = {
    val (subscription, doneF) = alarmService
      .subscribeAggregatedHealth(options.key)
      .toMat(Sink.foreach(health ⇒ printLine(Formatter.formatHealth(options.key, health))))(Keep.both)
      .run()

    unsubscribeOnCoordinatedShutdown(subscription)
    (subscription, doneF)
  }

  def unsubscribeOnCoordinatedShutdown(subscription: AlarmSubscription): Unit =
    coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "unsubscribe-health-stream") { () ⇒
      subscription.unsubscribe().map(_ ⇒ Done)
    }
}
