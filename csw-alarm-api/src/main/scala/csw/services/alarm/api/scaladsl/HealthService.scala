package csw.services.alarm.api.scaladsl
import akka.actor.typed.ActorRef
import csw.services.alarm.api.models.{AlarmHealth, Key}

import scala.concurrent.Future

trait HealthService {
  def getAggregatedHealth(key: Key): Future[AlarmHealth]
  def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription
  def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription
}
