package csw.services.alarm.api.scaladsl

import akka.actor.typed.ActorRef
import csw.services.alarm.api.models._

import scala.concurrent.Future

trait AlarmAdminService extends AlarmService {
  def getSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getMetadata(key: AlarmKey): Future[AlarmMetadata]
  def getMetadata(
      subsystem: Option[String] = None,
      componentName: Option[String] = None,
      alarmName: Option[String] = None
  ): Future[List[AlarmMetadata]]
  def getStatus(key: AlarmKey): Future[AlarmStatus]
  def acknowledge(key: AlarmKey): Future[Unit]
  def reset(key: AlarmKey): Future[Unit]
  def shelve(key: AlarmKey): Future[Unit]
  def unShelve(key: AlarmKey): Future[Unit]
  def activate(key: AlarmKey): Future[Unit]   // api only for test purpose
  def deActivate(key: AlarmKey): Future[Unit] // api only for test purpose
  def getSeverityAggregate(
      subsystem: Option[String] = None,
      componentName: Option[String] = None,
      alarmName: Option[String] = None
  ): Future[AlarmSeverity]
  def subscribeSeverityAggregateCallback(
      subsystem: Option[String] = None,
      componentName: Option[String] = None,
      alarmName: Option[String] = None,
      callback: AlarmSeverity ⇒ Unit
  ): Future[Unit]
  def subscribeSeverityAggregateActorRef(
      subsystem: Option[String] = None,
      componentName: Option[String] = None,
      alarmName: Option[String] = None,
      actorRef: ActorRef[AlarmSeverity]
  ): Future[Unit]
}
