package csw.services.integtration.crdt

import csw.services.location.scaladsl.ActorRuntime

object TestCrdtApp {
  import org.scalatest

  def main(args: Array[String]): Unit = {
    val actorRuntime = new ActorRuntime("crdt")
    scalatest.run(new LocationServiceIntegrationCrdtTest(actorRuntime))
  }
}
