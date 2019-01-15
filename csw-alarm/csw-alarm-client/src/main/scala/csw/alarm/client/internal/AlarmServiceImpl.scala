package csw.alarm.client.internal

import akka.actor.ActorSystem
import csw.alarm.api.scaladsl.AlarmAdminService
import csw.alarm.client.internal.commons.Settings
import csw.alarm.client.internal.redis.RedisConnectionsFactory
import csw.alarm.client.internal.services._

import scala.concurrent.ExecutionContext

class AlarmServiceImpl(
    override val redisConnectionsFactory: RedisConnectionsFactory,
    override val settings: Settings
)(
    implicit override val actorSystem: ActorSystem,
    val ec: ExecutionContext
) extends AlarmAdminService
    with MetadataServiceModule
    with SeverityServiceModule
    with StatusServiceModule
    with HealthServiceModule
