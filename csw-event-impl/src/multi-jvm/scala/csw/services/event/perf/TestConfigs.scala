package csw.services.event.perf

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class TestConfigs(config: Config) {

  //################### Common Configuration ###################
  val throttlingElements: Int = config.getInt("csw.test.EventServicePerfTest.throttling.elements")
  val throttlingDuration: FiniteDuration = {
    val d = config.getDuration("csw.test.EventServicePerfTest.throttling.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }
  val warmup: Int                 = config.getInt("csw.test.EventServicePerfTest.warmup")
  val totalMessagesFactor: Double = config.getDouble("csw.test.EventServicePerfTest.totalMessagesFactor")

  //################### Redis Configuration ###################
  lazy val redisEnabled: Boolean = config.getBoolean("csw.test.EventServicePerfTest.redis-enabled")
  lazy val redisHost: String     = config.getString("csw.test.EventServicePerfTest.redis.host")
  lazy val redisPort: Int        = config.getInt("csw.test.EventServicePerfTest.redis.port")

  //################### Kafka Configuration ###################
  lazy val kafkaHost: String = config.getString("csw.test.EventServicePerfTest.kafka.host")
  lazy val kafkaPort: Int    = config.getInt("csw.test.EventServicePerfTest.kafka.port")

}
