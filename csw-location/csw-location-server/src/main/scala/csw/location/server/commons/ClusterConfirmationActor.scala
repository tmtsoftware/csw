/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.commons

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.ClusterEvent.*
import org.apache.pekko.cluster.typed.{Cluster, Subscribe, Unsubscribe}
import csw.location.server.commons.ClusterConfirmationMessages.{HasJoinedCluster, Shutdown}

private[location] object ClusterConfirmationActor {

  def behavior(): Behavior[Any] =
    Behaviors.setup { ctx =>
      val cluster: Cluster = Cluster(ctx.system)
      cluster.subscriptions ! Subscribe(ctx.self, classOf[MemberEvent])

      def receiveBehavior(state: Option[Done] = None): Behaviors.Receive[Any] =
        Behaviors.receiveMessage[Any] {
          case MemberUp(member) if member.address == cluster.selfMember.address       => receiveBehavior(Some(Done))
          case MemberWeaklyUp(member) if member.address == cluster.selfMember.address => receiveBehavior(Some(Done))
          case HasJoinedCluster(ref)                                                  => ref ! state; Behaviors.same
          case Shutdown => Behaviors.stopped(() => cluster.subscriptions ! Unsubscribe(ctx.self))
          case _        => Behaviors.same
        }
      receiveBehavior()
    }

}

object ClusterConfirmationMessages {
  case class HasJoinedCluster(ref: ActorRef[Option[Done]])
  case object Shutdown
}
