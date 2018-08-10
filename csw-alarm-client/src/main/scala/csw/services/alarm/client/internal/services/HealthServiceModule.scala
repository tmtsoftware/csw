package csw.services.alarm.client.internal.services

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.alarm.api.models.{AlarmHealth, Key}
import csw.services.alarm.api.scaladsl.{AlarmSubscription, HealthService}
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory

import scala.concurrent.Future

trait HealthServiceModule extends HealthService {
  self: SeverityServiceModule ⇒

  val redisConnectionsFactory: RedisConnectionsFactory
  implicit val actorSystem: ActorSystem

  import redisConnectionsFactory._

  private val log                        = AlarmServiceLogger.getLogger
  implicit private val mat: Materializer = ActorMaterializer()

  final override def getAggregatedHealth(key: Key): Future[AlarmHealth] = {
    log.debug(s"Get aggregated health for alarm [${key.value}]")
    getAggregatedSeverity(key).map(AlarmHealth.fromSeverity)
  }

  final override def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with a callback")
    subscribeAggregatedSeverity(key)
      .map(AlarmHealth.fromSeverity)
      .to(Sink.foreach(callback))
      .run()
  }

  final override def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with an actor")
    subscribeAggregatedHealthCallback(key, actorRef ! _)
  }
}
