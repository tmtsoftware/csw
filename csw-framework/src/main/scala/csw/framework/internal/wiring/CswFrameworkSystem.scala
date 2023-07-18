/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.wiring

import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed._
import org.apache.pekko.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * A convenient class for creating a `typed` actor from the provided `untyped` actor system.
 * It creates an actor `CswFrameworkGuardian` from the provided actor system which spawns any other required actor
 * as its child. This is required because the default supervision strategy for an actor created from untyped actor
 * system is to `restart` the underlying actor but we want the default supervision strategy of `stopping` the actor
 * as provided in the `typed` actor world.
 */
private[csw] class CswFrameworkSystem(val system: ActorSystem[SpawnProtocol.Command]) {
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout     = Timeout(2.seconds)
  def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] =
    system ? (Spawn(behavior, name, props, _))
}
