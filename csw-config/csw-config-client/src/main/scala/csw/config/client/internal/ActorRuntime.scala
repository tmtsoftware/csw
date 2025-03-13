/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.client.internal

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
private[csw] class ActorRuntime(_typedSystem: ActorSystem[?] = ActorSystem(SpawnProtocol(), "config-client-system")) {
  implicit val actorSystem: ActorSystem[?]  = _typedSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext

  /**
   * The shutdown method helps self node to gracefully quit the pekko cluster. It is used by `csw-config-cli`
   * to shutdown the the app gracefully. `csw-config-cli` becomes the part of pekko cluster on booting up and
   * resolves the config server, using location service, to provide cli features around admin api of config service.
   *
   * @return a future that completes when shutdown is successful
   */
  def shutdown(): Future[Done] = {
    actorSystem.terminate()
    actorSystem.whenTerminated
  }
}
