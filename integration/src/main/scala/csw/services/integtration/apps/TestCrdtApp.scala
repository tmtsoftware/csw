package csw.services.integtration.apps

import csw.services.integtration.crdt.LocationServiceIntegrationCrdtTest

object TestCrdtApp {
  import org.scalatest

  def main(args: Array[String]): Unit = {
    scalatest.run(new LocationServiceIntegrationCrdtTest)
  }
}
