package csw.services

import csw.config.server.{Main => ConfigMain}

object ConfigServer {
  private val initSvnRepo = "--initRepo"

  def start(configPort: String): Unit = ConfigMain.main(Array("--port", configPort, initSvnRepo))
}
