package csw.services.integtration.apps

import csw.services.integtration.tests.LocationServiceIntegrationTest

object TestApp {
  import org.scalatest

  def main(args: Array[String]): Unit =
    scalatest.run(new LocationServiceIntegrationTest())
}
