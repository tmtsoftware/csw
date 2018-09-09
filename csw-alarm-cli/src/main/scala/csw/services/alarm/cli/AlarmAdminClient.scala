package csw.services.alarm.cli
import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.ConfigFactory
import csw.messages.location.scaladsl.LocationService
import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.scaladsl.AlarmSubscription
import csw.services.alarm.cli.args.Options
import csw.services.alarm.cli.extensions.RichFutureExt.RichFuture
import csw.services.alarm.cli.utils.{ConfigUtils, Formatter}
import csw.services.alarm.cli.wiring.ActorRuntime
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.alarm.client.internal.AlarmServiceImpl
import csw.services.alarm.client.internal.auto_refresh.AutoRefreshSeverityMessage.AutoRefreshSeverity
import csw.services.alarm.client.internal.auto_refresh.{AutoRefreshSeverityActorFactory, AutoRefreshSeverityMessage}
import csw.services.alarm.client.internal.commons.Settings

import scala.async.Async.{async, await}
import scala.concurrent.Future

class AlarmAdminClient(
    actorRuntime: ActorRuntime,
    locationService: LocationService,
    configUtils: ConfigUtils,
    printLine: Any ⇒ Unit
) {
  import actorRuntime._

  private[alarm] val alarmService: AlarmServiceImpl = new AlarmServiceFactory().makeAlarmImpl(locationService)

  def init(options: Options): Future[Done] =
    async {
      val config = await(configUtils.getConfig(options.isLocal, options.filePath, None))
      await(alarmService.initAlarms(config, options.reset))
    }.transformWithSideEffect(printLine)

  def getSeverity(options: Options): Future[Unit] = async {
    val severity = await(alarmService.getAggregatedSeverity(options.key))
    printLine(Formatter.formatAggregatedSeverity(options.key, severity))
  }

  def setSeverity(options: Options): Future[Done] =
    alarmService.setSeverity(options.alarmKey, options.severity.get).transformWithSideEffect(printLine)

  def refreshSeverity(options: Options): ActorRef[AutoRefreshSeverityMessage] = {
    val refreshInterval = new Settings(ConfigFactory.load()).refreshInterval
    def refreshable(key: AlarmKey, severity: AlarmSeverity): Unit =
      alarmService.setCurrentSeverity(key, severity).map(_ => printLine(Formatter.formatRefreshSeverity(key, severity)))

    val refreshActor = new AutoRefreshSeverityActorFactory().make(refreshable, refreshInterval)
    refreshActor ! AutoRefreshSeverity(options.alarmKey, options.severity.get)
    refreshActor
  }

  def subscribeSeverity(options: Options): (AlarmSubscription, Future[Done]) = {
    val (subscription, doneF) = alarmService
      .subscribeAggregatedSeverity(options.key)
      .toMat(Sink.foreach(severity ⇒ printLine(Formatter.formatAggregatedSeverity(options.key, severity))))(Keep.both)
      .run()

    unsubscribeOnCoordinatedShutdown(subscription)

    (subscription, doneF)
  }

  def acknowledge(options: Options): Future[Done] =
    alarmService.acknowledge(options.alarmKey).transformWithSideEffect(printLine)

  def unacknowledge(options: Options): Future[Done] =
    alarmService.unacknowledge(options.alarmKey).transformWithSideEffect(printLine)

  def activate(options: Options): Future[Done] =
    alarmService.activate(options.alarmKey).transformWithSideEffect(printLine)

  def deactivate(options: Options): Future[Done] =
    alarmService.deactivate(options.alarmKey).transformWithSideEffect(printLine)

  def shelve(options: Options): Future[Done] =
    alarmService.shelve(options.alarmKey).transformWithSideEffect(printLine)

  def unshelve(options: Options): Future[Done] =
    alarmService.unshelve(options.alarmKey).transformWithSideEffect(printLine)

  def reset(options: Options): Future[Done] =
    alarmService.reset(options.alarmKey).transformWithSideEffect(printLine)

  def list(options: Options): Future[Unit] = async {
    val alarms = await(alarmService.getAlarms(options.key)).sortWith(_.key.value > _.key.value)
    printLine(Formatter.formatAlarms(alarms, options))
  }

  def getHealth(options: Options): Future[Unit] = async {
    val health = await(alarmService.getAggregatedHealth(options.key))
    printLine(Formatter.formatAggregatedHealth(options.key, health))
  }

  def subscribeHealth(options: Options): (AlarmSubscription, Future[Done]) = {
    val (subscription, doneF) = alarmService
      .subscribeAggregatedHealth(options.key)
      .toMat(Sink.foreach(health ⇒ printLine(Formatter.formatAggregatedHealth(options.key, health))))(Keep.both)
      .run()

    unsubscribeOnCoordinatedShutdown(subscription)
    (subscription, doneF)
  }

  private def unsubscribeOnCoordinatedShutdown(subscription: AlarmSubscription): Unit =
    coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "unsubscribe-health-stream") { () ⇒
      subscription.unsubscribe()
    }
}
