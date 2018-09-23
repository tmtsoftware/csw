package csw.admin.internal

import com.typesafe.config.Config

class Settings(config: Config) {

  private val adminConfig = config.getConfig("csw-admin")

  def adminPort: Int = adminConfig.getInt("port")
}
