/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.commons.redis.EmbeddedRedis
import csw.event.client.helpers.TestFutureExt.given
import scala.language.implicitConversions

import csw.location.api.models
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.SocketUtils
import redis.embedded.{RedisSentinel, RedisServer}

class FrameworkTestWiring(val seedPort: Int = SocketUtils.getFreePort) extends EmbeddedRedis {

  implicit val seedActorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "seed-system")
  val seedLocationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient

  def startSentinelAndRegisterService(
      connection: TcpConnection,
      masterId: String
  ): (RegistrationResult, RedisSentinel, RedisServer) =
    withSentinel(masterId = masterId) { (sentinelPort, _) =>
      seedLocationService
        .register(models.TcpRegistration(connection, sentinelPort))
        .await
    }

  def shutdown(): Done = {
    seedActorSystem.terminate()
    seedActorSystem.whenTerminated.await
  }
}
