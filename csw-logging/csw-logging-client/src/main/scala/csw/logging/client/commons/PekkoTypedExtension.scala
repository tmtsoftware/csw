/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.commons

import org.apache.pekko.actor.typed.Scheduler
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object PekkoTypedExtension {
  implicit class UserActorFactory(system: ActorSystem[SpawnProtocol.Command]) {
    private val defaultDuration: FiniteDuration = 5.seconds
    private implicit val timeout: Timeout       = Timeout(defaultDuration)
    private implicit val scheduler: Scheduler   = system.scheduler

    def spawn[T](behavior: Behavior[T], name: String, props: Props = Props.empty): ActorRef[T] = {
      Await.result(system ? (Spawn(behavior, name, props, _)), defaultDuration)
    }
  }
}
