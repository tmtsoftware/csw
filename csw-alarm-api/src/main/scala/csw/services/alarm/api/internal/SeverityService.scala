package csw.services.alarm.api.internal
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{FullAlarmSeverity, Key}
import csw.services.alarm.api.scaladsl.{AlarmService, AlarmSubscription}

import scala.concurrent.Future

private[alarm] trait SeverityService extends AlarmService {
  def getCurrentSeverity(key: AlarmKey): Future[FullAlarmSeverity]
  def getAggregatedSeverity(key: Key): Future[FullAlarmSeverity]
  def subscribeAggregatedSeverityCallback(key: Key, callback: FullAlarmSeverity ⇒ Unit): AlarmSubscription
  def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[FullAlarmSeverity]): AlarmSubscription
  private[alarm] def subscribeAggregatedSeverity(key: Key): Source[FullAlarmSeverity, AlarmSubscription]
}
