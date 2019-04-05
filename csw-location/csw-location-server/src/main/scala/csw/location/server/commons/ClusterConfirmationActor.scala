package csw.location.server.commons

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ClusterEvent._
import akka.cluster.typed.{Cluster, Subscribe, Unsubscribe}
import csw.location.server.commons.ClusterConfirmationMessages.{HasJoinedCluster, Shutdown}

private[location] object ClusterConfirmationActor {

  def behavior(): Behavior[Any] = Behaviors.setup { ctx ⇒
    val cluster: Cluster = Cluster(ctx.system)
    cluster.subscriptions ! Subscribe(ctx.self, classOf[MemberEvent])

    def shutdownBehavior: Behavior[Any] = Behaviors.receiveMessage { _ ⇒
      cluster.subscriptions ! Unsubscribe(ctx.self); Behaviors.empty
    }

    def receiveBehavior(state: Option[Done] = None): Behaviors.Receive[Any] = Behaviors.receiveMessage[Any] {
      case MemberUp(member) if member.address == cluster.selfMember.address       ⇒ receiveBehavior(Some(Done))
      case MemberWeaklyUp(member) if member.address == cluster.selfMember.address ⇒ receiveBehavior(Some(Done))
      case HasJoinedCluster(ref)                                                  ⇒ ref ! state; Behaviors.same
      case Shutdown                                                               ⇒ Behaviors.stopped(shutdownBehavior)
      case _                                                                      ⇒ Behaviors.same
    }
    receiveBehavior()
  }

}

object ClusterConfirmationMessages {
  case class HasJoinedCluster(ref: ActorRef[Option[Done]])
  case object Shutdown
}
