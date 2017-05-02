package csw.services.location

import akka.Done
import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import csw.services.location.ClusterConfirmationActor.HasJoinedCluster

class ClusterConfirmationActor extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, InitialStateAsEvents, classOf[MemberEvent])
  override def postStop(): Unit = cluster.unsubscribe(self)

  var done: Option[Done] = None

  override def receive: Receive = {
    case MemberUp(member) if member.address == cluster.selfAddress     ⇒ done = Some(Done)
    case MemberJoined(member) if member.address == cluster.selfAddress ⇒ done = Some(Done)
    case HasJoinedCluster                                              ⇒ sender() ! done
  }

}

object ClusterConfirmationActor {
  def props() = Props(new ClusterConfirmationActor)

  case object HasJoinedCluster
}
