package csw.framework.models

import com.typesafe.config.Config

class Settings(config: Config) {
  private val crmConfig = config.getConfig("csw-command-client.mini-crm")

  def startedSize: Int  = crmConfig.getInt("started-size")
  def responseSize: Int = crmConfig.getInt("response-size")
  def waiterSize: Int   = crmConfig.getInt("waiter-size")
}
