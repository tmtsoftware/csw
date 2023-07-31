/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.commons

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import org.apache.pekko.cluster.ddata.SelfUniqueAddress
import org.apache.pekko.cluster.ddata.typed.scaladsl
import org.apache.pekko.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
import org.apache.pekko.cluster.typed.{Cluster, Join}
import org.apache.pekko.management.scaladsl.PekkoManagement
import org.apache.pekko.util.Timeout
import csw.location.api.exceptions.CouldNotJoinCluster
import csw.location.server.commons.ClusterConfirmationMessages.{HasJoinedCluster, Shutdown}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.network.utils.internal.BlockingUtils

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * CswCluster provides cluster properties to manage distributed data. It is created when location service instance is created
 * in `csw-framework` and joins the cluster.
 *
 * @note it is highly recommended that explicit creation of CswCluster should be for advanced usages or testing purposes only
 */
class CswCluster private (clusterSettings: ClusterSettings) {

  private val log: Logger = LocationServiceLogger.getLogger

  /**
   * Identifies the hostname where ActorSystem is running
   */
  val hostname: String            = clusterSettings.hostname
  lazy val publicHostname: String = clusterSettings.publicHostname

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = clusterSettings.system
  implicit val ec: ExecutionContext                            = actorSystem.executionContext
  implicit val cluster: Cluster                                = Cluster(actorSystem)
  private val distributedData: DistributedData                 = scaladsl.DistributedData(actorSystem)
  implicit val node: SelfUniqueAddress                         = distributedData.selfUniqueAddress

  /**
   * Gives the replicator for the current ActorSystem
   */
  private[location] val replicator: ActorRef[Replicator.Command] = distributedData.replicator

  /**
   * If `startManagement` flag is set to true (which is true only when a managementPort is defined in ClusterSettings)
   * then an pekko provided HTTP service is started at provided port. It provides services related to pekko cluster management e.g see the members of the cluster and their status i.e. up or weakly up etc.
   * Currently, cluster management service is started on `csw-location-server` which may help in production to monitor
   * cluster status. But, it can be started on any machine that is a part of pekko cluster.
   */
  // $COVERAGE-OFF$
  private def startClusterManagement(): Unit = {
    val startManagement = actorSystem.settings.config.getBoolean("startManagement")
    if (startManagement) {
      val pekkoManagement = PekkoManagement(actorSystem)
      Await.result(pekkoManagement.start(), 10.seconds)
    }
  }
  // $COVERAGE-ON$

  // When new member tries to join the cluster, location service makes sure that member is weakly up or up before returning handle to location service
  private def joinCluster(): Done = {
    // Check if seed nodes are provided to join csw-cluster
    val emptySeeds = actorSystem.settings.config.getStringList("pekko.cluster.seed-nodes").isEmpty
    if (emptySeeds) {
      // If no seeds are provided (which happens only during testing), then create a single node cluster by joining to self
      cluster.manager ! Join(cluster.selfMember.address)
    }

    val confirmationActorF: ActorRef[Any] = actorSystem.spawn(ClusterConfirmationActor.behavior(), "ClusterConfirmationActor")
    implicit val timeout: Timeout         = Timeout(5.seconds)
    def statusF: Future[Option[Done]]     = confirmationActorF ? HasJoinedCluster
    def status: Option[Done]              = Await.result(statusF, 5.seconds)
    val success                           = BlockingUtils.poll(status.isDefined, 20.seconds)
    if (!success) {
      val couldNotJoinCluster = CouldNotJoinCluster()
      log.error(couldNotJoinCluster.getMessage, ex = couldNotJoinCluster)
      throw couldNotJoinCluster
    }
    confirmationActorF ! Shutdown
    Done
  }

  /**
   * Terminates the ActorSystem and gracefully leaves the cluster
   */
  def shutdown(): Future[Done] = {
    actorSystem.terminate()
    actorSystem.whenTerminated
  }
}

/**
 * Manages initialization and termination of ActorSystem and the Cluster.
 *
 * @note the creation of CswCluster will be blocked till the ActorSystem joins csw-cluster successfully
 */
object CswCluster {

  private val log: Logger = LocationServiceLogger.getLogger

  /**
   * Creates CswCluster with the given customized settings
   *
   * @return an instance of CswCluster
   */
  def make(settings: ClusterSettings): CswCluster = {
    val cswCluster = new CswCluster(settings)
    try {
      cswCluster.startClusterManagement()
      cswCluster.joinCluster()
      cswCluster
    }
    catch {
      case NonFatal(ex) =>
        Await.result(cswCluster.shutdown(), 10.seconds)
        log.error(ex.getMessage, ex = ex)
        throw ex
    }
  }
}
