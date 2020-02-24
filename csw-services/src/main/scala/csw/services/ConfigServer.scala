package csw.services

import csw.config.server.http.HttpService
import csw.config.server.{ServerWiring, Main => ConfigMain}

object ConfigServer {
  private val initSvnRepo = "--initRepo"

  def start(configPort: String): Option[(HttpService, ServerWiring)] =
    Service.start("Config Service", ConfigMain.start(Array("--port", configPort, initSvnRepo)))
}
