package csw.services.alarm.api.internal
import akka.actor.typed.ActorRef
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmSeverity, Key}
import csw.services.alarm.api.scaladsl.{AlarmService, AlarmSubscription}

import scala.concurrent.Future

private[alarm] trait SeverityService extends AlarmService {
  def getCurrentSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getAggregatedSeverity(key: Key): Future[AlarmSeverity]
  def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription
  def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription
}
