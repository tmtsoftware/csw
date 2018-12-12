package csw.database.client

import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.jooq.impl.DSL
import org.jooq.{DSLContext, SQLDialect}
import csw.database.client.commons.DatabaseLogger
import csw.logging.scaladsl.Logger

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.Try
import scala.util.control.NonFatal

class DatabaseService(config: Config) {

  val logger: Logger = DatabaseLogger.getLogger
  // create connection with default read access
  def createDsl(dbName: String): DSLContext = createDsl(dbName, "dbReadUsername", "dbReadPassword")

  // create connection with credentials picked up from env variables
  def createDsl(dbName: String, usernameHolder: String, passwordHolder: String): DSLContext = {
    val databaseConfig: Config   = config.getConfig("csw-database")
    val dataSourceConfig: Config = databaseConfig.getConfig("hikari-datasource")

    // Get credentials from environment variable. If not present then get the default credentials from config file that gives
    // read access to users.
    val databaseCredentials = Map(
      "dataSource.databaseName" → dbName,
      "dataSource.user"         → sys.env.getOrElse(usernameHolder, dataSourceConfig.getString("dataSource.user")),
      "dataSource.password"     → sys.env.getOrElse(passwordHolder, dataSourceConfig.getString("dataSource.password"))
    )

    val finalDataSourceConfig = ConfigFactory
      .parseMap(databaseCredentials.asJava)
      .withFallback(dataSourceConfig)

    logger.info(s"datasource config = $finalDataSourceConfig")

    val hikariConfig = new HikariConfig(toProperties(finalDataSourceConfig))
    val dialect      = databaseConfig.getString("databaseDialect")

    Try(DSL.using(new HikariDataSource(hikariConfig), SQLDialect.valueOf(dialect))).recover {
      case NonFatal(ex) =>
        logger.warn(s"could not connect to dbName = '$dbName', falling back to default database from the config")
        val defaultHikariConfig = new HikariConfig(toProperties(dataSourceConfig))
        logger.warn(s"default config = $dataSourceConfig")
        DSL.using(new HikariDataSource(defaultHikariConfig), SQLDialect.valueOf(dialect))
    }.get
  }

  private def toProperties(config: Config): Properties = {
    val properties = new Properties()
    config.entrySet.forEach { e =>
      properties.setProperty(e.getKey, config.getString(e.getKey))
    }
    properties
  }
}
