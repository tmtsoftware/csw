package csw.services.alarm.api.scaladsl

import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models._

import scala.concurrent.Future

trait AlarmAdminService extends AlarmService {

  def initAlarms(inputConfig: Config, reset: Boolean = false): Future[Unit]

  def getCurrentSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getStatus(key: AlarmKey): Future[AlarmStatus]
  def getMetadata(key: AlarmKey): Future[AlarmMetadata]
  def getMetadata(key: Key): Future[List[AlarmMetadata]]
  def acknowledge(key: AlarmKey): Future[Unit]
  def reset(key: AlarmKey): Future[Unit]
  def shelve(key: AlarmKey): Future[Unit]
  def unShelve(key: AlarmKey): Future[Unit]
  def activate(key: AlarmKey): Future[Unit]   // api only for test purpose
  def deActivate(key: AlarmKey): Future[Unit] // api only for test purpose

  def getAggregatedSeverity(key: Key): Future[AlarmSeverity]
  def getAggregatedHealth(key: Key): Future[AlarmHealth]

  def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription
  def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription

  def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription
  def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription

  private[alarm] def unAcknowledge(key: AlarmKey): Future[Unit]
}
