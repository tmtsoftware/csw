/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.agent.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.agent.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

private[agent] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = actorSystem.executionContext
  implicit val scheduler: Scheduler                            = actorSystem.scheduler

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, actorSystem)

  def shutdown(): Future[Done] = {
    actorSystem.terminate()
    actorSystem.whenTerminated
  }
}
