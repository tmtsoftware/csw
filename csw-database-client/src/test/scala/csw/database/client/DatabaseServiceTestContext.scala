package csw.database.client
import java.nio.file.Paths

import akka.actor.ActorSystem
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.typesafe.config.{Config, ConfigFactory}

object DatabaseServiceTestContext {
  def postgres(): EmbeddedPostgres =
    EmbeddedPostgres.builder
      .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
      .setCleanDataDirectory(true)
      .setPgBinaryResolver(new PostgresBinaryResolver)
      .start

  def config(system: ActorSystem, port: Int): Config = {
    val portConfig = s"csw-database.hikari-datasource.dataSource.portNumber = $port"
    ConfigFactory.parseString(portConfig).withFallback(system.settings.config)
  }
}
