package csw.services.integtration.apps

import csw.services.integtration.tests.LocationServiceMultipleNICTest
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.CswCluster
import org.scalatest

object TestMultipleNicApp {
  def main(args: Array[String]): Unit = {
    val cswCluster = CswCluster.withSettings(Settings().withInterface("eth1"))
    scalatest.run(new LocationServiceMultipleNICTest(cswCluster))
  }
}
