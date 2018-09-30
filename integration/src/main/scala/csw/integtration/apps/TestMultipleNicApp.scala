package csw.integtration.apps

import csw.integtration.tests.LocationServiceMultipleNICTest
import org.scalatest

object TestMultipleNicApp {
  def main(args: Array[String]): Unit = {
    scalatest.run(new LocationServiceMultipleNICTest())
  }
}
