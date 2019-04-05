package csw.command.client

import java.time.Duration

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Configurations to be used by underlying `CommandResponseState's` cache maintained by [[CommandResponseManagerActor]]
 *
 * @param maxSize maximum number of states the cache can hold, used for eviction of states
 * @param expiry maximum amount of time a state can stay in cache from last access, also used for eviction
 */
case class CRMCacheProperties(maxSize: Int, expiry: Duration)

object CRMCacheProperties {

  /**
   * constructor for creating cache properties
   *
   * @param config custom configuration for cache properties
   * @return cache properties to be used by [[CommandResponseManagerActor]]
   */
  def apply(config: Config = ConfigFactory.load()): CRMCacheProperties = {
    val crmConfig = config.getConfig("csw-command-client.command-response-state")
    CRMCacheProperties(crmConfig.getInt("maximum-size"), crmConfig.getDuration("expiry"))
  }
}
