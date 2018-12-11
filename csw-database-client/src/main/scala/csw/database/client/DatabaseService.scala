package csw.database.client

import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.jooq.impl.DSL
import org.jooq.{DSLContext, SQLDialect}

import scala.collection.JavaConverters.mapAsJavaMapConverter

class DatabaseService(config: Config) {

  // default connection provides read access
  val defaultDSL: DSLContext = createDsl()

  // create connection with default read access
  def createDsl(): DSLContext = createDSL("dbReadUsername", "dbReadPassword")

  // create connection with credentials picked up from env variables
  def createDSL(usernameHolder: String, passwordHolder: String): DSLContext = {
    val databaseConfig: Config   = config.getConfig("csw-database")
    val dataSourceConfig: Config = databaseConfig.getConfig("hikari-datasource")

    // Get credentials from environment variable. If not present then get the default credentials from config file that gives
    // read access to users.
    val credentials = Map(
      "dataSource.user"     → sys.env.getOrElse(usernameHolder, dataSourceConfig.getString("dataSource.user")),
      "dataSource.password" → sys.env.getOrElse(passwordHolder, dataSourceConfig.getString("dataSource.password"))
    )

    val finalDataSourceConfig = ConfigFactory
      .parseMap(credentials.asJava)
      .withFallback(dataSourceConfig)

    val hikariConfig = new HikariConfig(toProperties(finalDataSourceConfig))
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
