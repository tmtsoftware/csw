package csw.config.cli.wiring
import java.nio.file.{Path, Paths}

import com.typesafe.config.Config

class Settings(config: Config) {

  private val `csw-config-cli` = config.getConfig("csw-config-cli")

  val `auth-store-dir`: String = `csw-config-cli`.getString("auth-store-dir")
  val authStorePath: Path      = Paths.get(`auth-store-dir`)
}
