/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.redis

import org.apache.pekko.actor.typed.ActorSystem
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.serviceresolver.EventServiceResolver
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.Future

/**
 * Implementation of [[csw.event.api.scaladsl.EventService]] which provides handle to [[csw.event.api.scaladsl.EventPublisher]]
 * and [[csw.event.api.scaladsl.EventSubscriber]] backed by Redis
 *
 * @param eventServiceResolver to get the connection information of event service
 * @param masterId the Id used by Redis Sentinel to identify the master
 * @param redisClient the client instance of [[io.lettuce.core.RedisClient]]
 * @param actorSystem provides Materializer, ExecutionContext, etc.
 */
private[event] class RedisEventService(eventServiceResolver: EventServiceResolver, masterId: String, redisClient: RedisClient)(
    implicit val actorSystem: ActorSystem[?]
) extends EventService {

  import actorSystem.executionContext

  override def makeNewPublisher(): RedisPublisher = new RedisPublisher(redisURI(), redisClient)

  override def makeNewSubscriber(): RedisSubscriber = new RedisSubscriber(redisURI(), redisClient)

  // resolve event service every time before creating a new publisher or subscriber
  private def redisURI(): Future[RedisURI] =
    eventServiceResolver.uri().map(uri => RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build())

}
