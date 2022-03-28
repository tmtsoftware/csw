/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal

import akka.actor.typed
import csw.alarm.api.scaladsl.AlarmAdminService
import csw.alarm.client.internal.commons.Settings
import csw.alarm.client.internal.redis.RedisConnectionsFactory
import csw.alarm.client.internal.services._

import scala.concurrent.ExecutionContext

private[alarm] class AlarmServiceImpl(
    override val redisConnectionsFactory: RedisConnectionsFactory,
    override val settings: Settings
)(implicit
    override val actorSystem: typed.ActorSystem[_],
    val ec: ExecutionContext
) extends AlarmAdminService
    with MetadataServiceModule
    with SeverityServiceModule
    with StatusServiceModule
    with HealthServiceModule
