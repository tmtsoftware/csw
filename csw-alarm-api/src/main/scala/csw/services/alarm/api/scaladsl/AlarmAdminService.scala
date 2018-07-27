package csw.services.alarm.api.scaladsl

import akka.actor.typed.ActorRef
import csw.services.alarm.api.models._

import scala.concurrent.Future

trait AlarmAdminService extends AlarmService {
  def getSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getStatus(key: AlarmKey): Future[AlarmStatus]
  def getMetadata(key: AlarmKey): Future[List[AlarmMetadata]]
  def acknowledge(key: AlarmKey): Future[Unit]
  def reset(key: AlarmKey): Future[Unit]
  def shelve(key: AlarmKey): Future[Unit]
  def unShelve(key: AlarmKey): Future[Unit]
  def activate(key: AlarmKey): Future[Unit]   // api only for test purpose
  def deActivate(key: AlarmKey): Future[Unit] // api only for test purpose

  def getAggregatedSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getAggregatedHealth(key: AlarmKey): Future[AlarmHealth]

  def subscribeAggregatedSeverityCallback(key: AlarmKey, callback: AlarmSeverity ⇒ Unit): AlarmSubscription
  def subscribeAggregatedHealthCallback(key: AlarmKey, callback: AlarmHealth ⇒ Unit): AlarmSubscription

  def subscribeAggregatedSeverityActorRef(key: AlarmKey, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription
  def subscribeAggregatedHealthActorRef(key: AlarmKey, actorRef: ActorRef[AlarmHealth]): AlarmSubscription
}
