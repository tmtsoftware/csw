package csw.services.event.perf.wiring

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class TestConfigs(config: Config) {

  //################### Common Configuration ###################
  val elements: Int = config.getInt("csw.event.perf.publish-frequency.elements")
  val per: FiniteDuration = {
    val d = config.getDuration("csw.event.perf.publish-frequency.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }

  val publishFrequency: FiniteDuration = (per.toMillis / elements).millis

  val warmupMsgs: Int             = config.getInt("csw.event.perf.warmup")
  val burstSize: Int              = config.getInt("csw.event.perf.burst-size")
  val totalMessagesFactor: Double = config.getDouble("csw.event.perf.totalMessagesFactor")

  val shareConnection: Boolean = config.getBoolean("csw.event.perf.one-connection-per-jvm")

  //################### Redis Configuration ###################
  val redisEnabled: Boolean = config.getBoolean("csw.event.perf.redis-enabled")
  val redisHost: String     = config.getString("csw.event.perf.redis.host")
  val redisPort: Int        = config.getInt("csw.event.perf.redis.port")

  //################### Kafka Configuration ###################
  val kafkaHost: String = config.getString("csw.event.perf.kafka.host")
  val kafkaPort: Int    = config.getInt("csw.event.perf.kafka.port")

  val systemMonitoring: Boolean = config.getBoolean("csw.event.perf.system-monitoring")
}
