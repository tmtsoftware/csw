package csw.event.client.perf.wiring

import com.typesafe.config.Config

class TestConfigs(config: Config) {

  //################### Common Configuration ###################
  val frequency: Int = config.getInt("csw.event.client.perf.publish-frequency")

  val warmupMsgs: Int             = config.getInt("csw.event.client.perf.warmup")
  val burstSize: Int              = config.getInt("csw.event.client.perf.burst-size")
  val totalMessagesFactor: Double = config.getDouble("csw.event.client.perf.totalMessagesFactor")

  val shareConnection: Boolean          = config.getBoolean("csw.event.client.perf.one-connection-per-jvm")
  val patternBasedSubscription: Boolean = config.getBoolean("csw.event.client.perf.pattern-based-subscription")

  //################### Redis Configuration ###################
  val redisEnabled: Boolean = config.getBoolean("csw.event.client.perf.redis-enabled")
  val redisHost: String     = config.getString("csw.event.client.perf.redis.host")
  val redisPort: Int        = config.getInt("csw.event.client.perf.redis.port")
  val redisPattern: String  = config.getString("csw.event.client.perf.redis.pattern-for-subscription")

  //################### Kafka Configuration ###################
  val kafkaHost: String    = config.getString("csw.event.client.perf.kafka.host")
  val kafkaPort: Int       = config.getInt("csw.event.client.perf.kafka.port")
  val kafkaPattern: String = config.getString("csw.event.client.perf.kafka.pattern-for-subscription")

  val systemMonitoring: Boolean = config.getBoolean("csw.event.client.perf.system-monitoring")
}
