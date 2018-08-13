package csw.services.alarm.api.scaladsl
import akka.actor.typed.ActorRef
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmSeverity, Key}

import scala.concurrent.Future

private[alarm] trait SeverityService {
  def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit]
  def getCurrentSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getAggregatedSeverity(key: Key): Future[AlarmSeverity]
  def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription
  def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription
}
