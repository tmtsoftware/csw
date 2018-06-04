package csw.services.event.perf.wiring

import com.typesafe.config.Config

class TestConfigs(config: Config) {

  //################### Common Configuration ###################
  val frequency: Int = config.getInt("csw.event.perf.publish-frequency")

  val warmupMsgs: Int             = config.getInt("csw.event.perf.warmup")
  val burstSize: Int              = config.getInt("csw.event.perf.burst-size")
  val totalMessagesFactor: Double = config.getDouble("csw.event.perf.totalMessagesFactor")

  val shareConnection: Boolean          = config.getBoolean("csw.event.perf.one-connection-per-jvm")
  val patternBasedSubscription: Boolean = config.getBoolean("csw.event.perf.pattern-based-subscription")

  //################### Redis Configuration ###################
  val redisEnabled: Boolean = config.getBoolean("csw.event.perf.redis-enabled")
  val redisHost: String     = config.getString("csw.event.perf.redis.host")
  val redisPort: Int        = config.getInt("csw.event.perf.redis.port")
  val redisPattern: String  = config.getString("csw.event.perf.redis.pattern-for-subscription")

  //################### Kafka Configuration ###################
  val kafkaHost: String    = config.getString("csw.event.perf.kafka.host")
  val kafkaPort: Int       = config.getInt("csw.event.perf.kafka.port")
  val kafkaPattern: String = config.getString("csw.event.perf.kafka.pattern-for-subscription")

  val systemMonitoring: Boolean = config.getBoolean("csw.event.perf.system-monitoring")
}
