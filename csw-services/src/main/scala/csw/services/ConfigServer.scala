package csw.services

import csw.config.server.{Main => ConfigMain}

object ConfigServer {
  private val initSvnRepo = "--initRepo"

  def start(configPort: String): Unit =
    Service.start("Config Service", ConfigMain.main(Array("--port", configPort, initSvnRepo)))
}
