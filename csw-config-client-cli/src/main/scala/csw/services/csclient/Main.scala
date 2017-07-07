package csw.services.csclient

import akka.Done
import akka.actor.{ActorSystem, PoisonPill}
import akka.cluster.Cluster
import akka.util.Timeout
import csw.services.BuildInfo
import csw.services.config.client.commons.ConfigClientLogger
import csw.services.csclient.cli.{ArgsParser, ClientCliWiring, Options}
import csw.services.location.commons.ClusterConfirmationActor.IsMemberUp
import csw.services.location.commons.{BlockingUtils, ClusterAwareSettings, ClusterConfirmationActor}
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// $COVERAGE-OFF$
object Main extends App with ConfigClientLogger.Simple {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    ArgsParser.parse(args) match {
      case None          ⇒
      case Some(options) ⇒ run(options)
    }
  }

  private def run(options: Options): Unit = {
    val actorSystem = ClusterAwareSettings.system
    LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new ClientCliWiring(actorSystem)
    try {
      wiring.cliApp.start(options)
    } finally {
      // Make sure that member is up when running coordinated shutdown
      if (isUp(actorSystem)) Await.ready(wiring.actorRuntime.shutdown(), 30.seconds)
      else {
        // this means that Member is not able to state change to Up status and it is most probably in WeaklyUp state
        // State transition from WeaklyUp cluster member is : WeaklyUp -> Unreachable -> Down -> Removed
        // todo there should be a better strategy to remove WeaklyUp member from cluster
        val cluster = Cluster(actorSystem)
        log.debug("Disconnecting from cluster as WeaklyUp member")
        cluster.down(cluster.selfAddress)
      }
    }
  }

  private def isUp(actorSystem: ActorSystem): Boolean = {
    import akka.pattern.ask

    val confirmationActor = actorSystem.actorOf(ClusterConfirmationActor.props())
    implicit val timeout  = Timeout(5.seconds)
    def statusF           = (confirmationActor ? IsMemberUp).mapTo[Option[Done]]
    def status            = Await.result(statusF, 5.seconds)
    val success           = BlockingUtils.poll(status.isDefined, 10.seconds)
    confirmationActor ! PoisonPill
    success
  }
}
// $COVERAGE-ON$
