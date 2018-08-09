package csw.services.alarm.client.internal

import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models._
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmSubscription}
import csw.services.alarm.client.internal.services.{HealthService, MetadataService, SeverityService, StatusService}

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class AlarmServiceImpl(
    healthService: HealthService,
    statusService: StatusService,
    metadataService: MetadataService,
    severityService: SeverityService
)(implicit ec: ExecutionContext)
    extends AlarmAdminService {

  override def initAlarms(inputConfig: Config, reset: Boolean): Future[Unit] = metadataService.initAlarms(inputConfig, reset)

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    val previousSeverity = await(severityService.getCurrentSeverity(key))
    await(severityService.setCurrentSeverity(key, severity))
    await(statusService.updateStatusForSeverity(key, severity, previousSeverity))
  }

  override def getCurrentSeverity(key: AlarmKey): Future[AlarmSeverity] = severityService.getCurrentSeverity(key)

  override def getStatus(key: AlarmKey): Future[AlarmStatus] = statusService.getStatus(key)

  override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = metadataService.getMetadata(key)

  override def getMetadata(key: Key): Future[List[AlarmMetadata]] = metadataService.getMetadata(key)

  override def acknowledge(key: AlarmKey): Future[Unit] = statusService.acknowledge(key)

  // reset is only called when severity is `Okay`
  override def reset(key: AlarmKey): Future[Unit] = statusService.reset(key)

  override def shelve(key: AlarmKey): Future[Unit] = statusService.shelve(key)

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  override def unShelve(key: AlarmKey): Future[Unit] = statusService.unShelve(key)

  override def activate(key: AlarmKey): Future[Unit] = metadataService.activate(key)

  override def deActivate(key: AlarmKey): Future[Unit] = metadataService.deActivate(key)

  override def getAggregatedSeverity(key: Key): Future[AlarmSeverity] = severityService.getAggregatedSeverity(key)

  override def getAggregatedHealth(key: Key): Future[AlarmHealth] = healthService.getAggregatedHealth(key)

  override def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription =
    severityService.subscribeAggregatedSeverityCallback(key, callback)

  override def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription =
    healthService.subscribeAggregatedHealthCallback(key, callback)

  override def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription =
    severityService.subscribeAggregatedSeverityActorRef(key, actorRef)

  override def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription =
    healthService.subscribeAggregatedHealthActorRef(key, actorRef)

  override private[alarm] def unAcknowledge(key: AlarmKey): Future[Unit] = statusService.unAcknowledge(key)

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Unit] =
    statusService.setStatus(alarmKey, alarmStatus)

  private[alarm] def setMetadata(alarmKey: AlarmKey, alarmMetadata: AlarmMetadata): Future[Unit] =
    metadataService.setMetadata(alarmKey, alarmMetadata)
}
