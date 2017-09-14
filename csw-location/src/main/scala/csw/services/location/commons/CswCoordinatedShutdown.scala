package csw.services.location.commons

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown, PoisonPill}
import akka.cluster.Cluster
import akka.util.Timeout
import csw.services.location.commons.ClusterConfirmationActor.IsMemberUp

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

object CswCoordinatedShutdown extends LocationServiceLogger.Simple {

  private def isMemberUp(actorSystem: ActorSystem): Boolean = {
    import akka.pattern.ask

    implicit val timeout: Timeout = Timeout(5.seconds)
    val confirmationActor         = actorSystem.actorOf(ClusterConfirmationActor.props())
    def statusF                   = (confirmationActor ? IsMemberUp).mapTo[Option[Done]]
    def status                    = Await.result(statusF, 5.seconds)
    val success                   = BlockingUtils.poll(status.isDefined, 10.seconds)
    confirmationActor ! PoisonPill
    success
  }

  def run(actorSystem: ActorSystem): Future[Done] = {
    if (CswCoordinatedShutdown.isMemberUp(actorSystem)) {
      log.info("Starting csw co-ordinated shutdown")
      CoordinatedShutdown(actorSystem).run
    } else {
      // this means that Member is not able to state change to Up status and it is most probably in Joining/WeaklyUp state
      // State transition for cluster member is: Joining -> WeaklyUp -> Unreachable -> Down -> Removed
      val cluster      = Cluster(actorSystem)
      val selfAddress  = cluster.selfAddress
      val memberStatus = cluster.state.members.find(_.address == selfAddress).map(_.status).getOrElse("")
      log.warn(s"Downing member: [$selfAddress] which is in state: [$memberStatus]")
      cluster.down(selfAddress)
      Future.successful(Done)
    }
  }
}
