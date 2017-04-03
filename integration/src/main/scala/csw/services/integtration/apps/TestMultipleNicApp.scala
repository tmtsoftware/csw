package csw.services.integtration.apps

import csw.services.integtration.tests.LocationServiceMultipleNICTest
import csw.services.location.commons.{ClusterSettings, CswCluster}
import org.scalatest

object TestMultipleNicApp {
  def main(args: Array[String]): Unit = {
    val cswCluster = CswCluster.withSettings(ClusterSettings().withInterface("eth1"))
    scalatest.run(new LocationServiceMultipleNICTest(cswCluster))
  }
}
