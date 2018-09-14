package csw.integtration.apps

import csw.integtration.tests.LocationServiceIntegrationTest

object TestApp {
  import org.scalatest

  def main(args: Array[String]): Unit =
    scalatest.run(new LocationServiceIntegrationTest())
}
