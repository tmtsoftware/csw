package csw.services.alarm.client.internal.services

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.alarm.api.models.{AlarmHealth, Key}
import csw.services.alarm.api.scaladsl.AlarmSubscription
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory

import scala.concurrent.Future

class HealthService(
    redisConnectionsFactory: RedisConnectionsFactory,
    severityService: SeverityService
)(implicit actorSystem: ActorSystem) {
  import redisConnectionsFactory._
  private val log                     = AlarmServiceLogger.getLogger
  implicit lazy val mat: Materializer = ActorMaterializer()

  def getAggregatedHealth(key: Key): Future[AlarmHealth] = {
    log.debug(s"Get aggregated health for alarm [${key.value}]")
    severityService.getAggregatedSeverity(key).map(AlarmHealth.fromSeverity)
  }

  def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with a callback")
    severityService
      .subscribeAggregatedSeverity(key)
      .map(AlarmHealth.fromSeverity)
      .to(Sink.foreach(callback))
      .run()
  }

  def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with an actor")
    subscribeAggregatedHealthCallback(key, actorRef ! _)
  }
}
