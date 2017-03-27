package csw.services.location.scaladsl

import
akka.actor.ActorSystem
import csw.services.location.internal.Settings
import org.scalatest.{FunSuite, Matchers}
import csw.services.location.common.TestFutureExtension.RichFuture

class ActorRuntimeTest
  extends FunSuite
    with Matchers {

  test("able to create default actor runtime") {
    //#actor-runtime-creation
    val actorRuntime = new ActorRuntime()
    //#actor-runtime-creation

    actorRuntime.isInstanceOf[ActorRuntime] shouldBe true
    actorRuntime.terminate().await
  }

  test("able to create actor runtime with custom settings") {
    //#actor-runtime-creation-with-Settings
    val port = 2556
    val settings = Settings().withPort(port)
    val actorRuntime = new ActorRuntime(settings)
    //#actor-runtime-creation-with-Settings
    actorRuntime.actorSystem.settings.config.getString("akka.remote.netty.tcp.port") shouldBe port.toString
    actorRuntime.terminate().await
  }

  test("able to create actor runtime with actor system") {
    //#actor-runtime-creation-with-system
    val actorSystem = ActorSystem("system-name")
    val actorRuntime = new ActorRuntime(actorSystem)
    //#actor-runtime-creation-with-system
    actorRuntime.actorSystem shouldBe actorSystem
    actorRuntime.terminate().await
  }
}
