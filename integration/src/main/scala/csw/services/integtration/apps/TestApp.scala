package csw.services.integtration.apps

import csw.services.integtration.tests.{LocationServiceIntegrationTest, TrackLocationAppIntegrationTest}
import csw.services.location.scaladsl.ActorRuntime

object TestApp {
  import org.scalatest

  def main(args: Array[String]): Unit = {
    val actorRuntime = new ActorRuntime("crdt")
    scalatest.run(new TrackLocationAppIntegrationTest(actorRuntime))
    scalatest.run(new LocationServiceIntegrationTest(actorRuntime))
  }
}
