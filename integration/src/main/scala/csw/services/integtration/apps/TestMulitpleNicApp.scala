package csw.services.integtration.apps

import csw.services.integtration.tests.LocationServiceMultipleNICTest
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.ActorRuntime
import org.scalatest

object TestMulitpleNicApp {
  def main(args: Array[String]): Unit = {
    scalatest.run(new LocationServiceMultipleNICTest(new ActorRuntime("crdt", Settings().withInterface("eth1"))))
  }
}
