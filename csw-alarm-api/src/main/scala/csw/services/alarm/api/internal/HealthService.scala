package csw.services.alarm.api.internal
import akka.actor.typed.ActorRef
import csw.services.alarm.api.models.{AlarmHealth, Key}
import csw.services.alarm.api.scaladsl.AlarmSubscription

import scala.concurrent.Future

private[alarm] trait HealthService {
  def getAggregatedHealth(key: Key): Future[AlarmHealth]
  def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription
  def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription
}
