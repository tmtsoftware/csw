package csw.services.event.perf

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class TestConfigs(implicit actorSystem: ActorSystem) {
  lazy val config: Config = actorSystem.settings.config

  val throttlingElements: Int = config.getInt("csw.test.EventServicePerfTest.throttling.elements")

  val throttlingDuration: FiniteDuration = {
    val d = config.getDuration("csw.test.EventServicePerfTest.throttling.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }

  val warmup: Int                 = config.getInt("csw.test.EventServicePerfTest.warmup")
  val totalMessagesFactor: Double = actorSystem.settings.config.getDouble("csw.test.EventServicePerfTest.totalMessagesFactor")

}
