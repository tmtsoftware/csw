/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.cli.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.alarm.cli.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit lazy val ec: ExecutionContextExecutor                    = actorSystem.executionContext

  lazy val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, actorSystem)

  /**
   * Gracefully shutdown [[_typedSystem]]
   *
   * @return a future that completes when shutdown is successful
   */
  def shutdown(): Future[Done] = {
    actorSystem.terminate()
    actorSystem.whenTerminated
  }
}
