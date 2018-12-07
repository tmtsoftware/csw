package csw.database.client.scaladsl
import java.util.Properties

import com.typesafe.config.Config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.jooq.impl.DSL
import org.jooq.{DSLContext, SQLDialect}

class DatabaseService(config: Config) {

  val defaultDSL: DSLContext = createDsl()

  def createDsl(): DSLContext = {
    val databaseConfig: Config   = config.getConfig("csw-database")
    val dataSourceConfig: Config = databaseConfig.getConfig("hikari-datasource")

    val hikariConfig = new HikariConfig(toProperties(dataSourceConfig))
    val dialect      = databaseConfig.getString("databaseDialect")

    DSL.using(new HikariDataSource(hikariConfig), SQLDialect.valueOf(dialect))
  }

  private def toProperties(config: Config): Properties = {
    val properties = new Properties()
    config.entrySet.forEach { e =>
      properties.setProperty(e.getKey, config.getString(e.getKey))
    }
    properties
  }
}
