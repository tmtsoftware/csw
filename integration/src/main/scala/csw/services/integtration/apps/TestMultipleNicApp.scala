package csw.services.integtration.apps

import csw.services.integtration.tests.LocationServiceMultipleNICTest
import csw.services.location.internal.Settings
import csw.services.location.scaladsl.ActorRuntime
import org.scalatest

object TestMultipleNicApp {
  def main(args: Array[String]): Unit = {
    val actorRuntime = new ActorRuntime(Settings().withInterface("eth1"))
    scalatest.run(new LocationServiceMultipleNICTest(actorRuntime))
    actorRuntime.terminate()
  }
}
