package csw.services.alarm.client.internal

import akka.actor.ActorSystem
import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.services._
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class AlarmServiceImpl(
    override val redisConnectionsFactory: RedisConnectionsFactory,
    override val shelveTimeoutActorFactory: ShelveTimeoutActorFactory,
    override val settings: Settings
)(
    implicit override val actorSystem: ActorSystem,
    val ec: ExecutionContext
) extends AlarmAdminService
    with MetadataServiceModule
    with SeverityServiceModule
    with StatusServiceModule
    with HealthServiceModule {

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    val previousSeverity = await(getCurrentSeverity(key))
    await(setCurrentSeverity(key, severity))
    await(updateStatusForSeverity(key, severity, previousSeverity))
  }

}
