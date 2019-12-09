package csw.admin.server.wiring

import com.typesafe.config.Config

class Settings(config: Config) {

  private val adminConfig = config.getConfig("csw-admin-server")

  def adminPort: Int = adminConfig.getInt("port")
}
