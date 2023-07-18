/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework.components.assembly

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import csw.config.api.ConfigData
import example.framework.components.assembly.WorkerActorMsgs.{GetStatistics, InitialState}

sealed trait WorkerActorMsg
object WorkerActorMsgs {
  case class InitialState(replyTo: ActorRef[Int])      extends WorkerActorMsg
  case class JInitialState(replyTo: ActorRef[Integer]) extends WorkerActorMsg
  case class GetStatistics(replyTo: ActorRef[Int])     extends WorkerActorMsg
}

object WorkerActor {

  def behavior(configData: ConfigData): Behavior[WorkerActorMsg] =
    Behaviors.setup { ctx =>
      // some setup required for the actor could be done here

      def receive(state: Int): Behavior[WorkerActorMsg] =
        Behaviors.receiveMessage {
          case _: InitialState  => receive(0) // do something and return new behavior with the changed state
          case _: GetStatistics => receive(1) // do something else
          case _                => Behaviors.same
        }

      receive(-1)
    }
}
