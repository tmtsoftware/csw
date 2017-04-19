package csw.apps.clusterseed

import csw.services.location.commons.CswCluster

object ClusterSeed {

  def main(args: Array[String]): Unit =
    CswCluster.make()

}
