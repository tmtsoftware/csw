package csw.database.client

import java.util.Properties
import java.util.concurrent.CompletableFuture

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import csw.database.client.commons.{DatabaseLogger, DatabaseServiceLocationResolver}
import csw.database.client.exceptions.DatabaseException
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService
import csw.logging.scaladsl.Logger
import org.jooq.impl.DSL
import org.jooq.{DSLContext, SQLDialect}

import scala.async.Async.{async, await}
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DatabaseService(actorSystem: ActorSystem[_]) {

  private val ReadUsernameHolder = "dbReadUsername"
  private val ReadPasswordHolder = "dbReadPassword"
  private val log: Logger        = DatabaseLogger.getLogger
  private val config             = actorSystem.settings.config

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  // create connection with default read access, throws DatabaseException
  def createDsl(locationService: LocationService, dbName: String): Future[DSLContext] =
    createDsl(locationService, dbName, ReadUsernameHolder, ReadPasswordHolder)

  // create connection with credentials picked up from env variables, throws DatabaseException
  def createDsl(
      locationService: LocationService,
      dbName: String,
      usernameHolder: String,
      passwordHolder: String
  ): Future[DSLContext] = async {
    val resolver = new DatabaseServiceLocationResolver(locationService)
    val uri      = await(resolver.uri())
    val dataSource: Map[String, Any] = Map(
      "dataSource.serverName"   → uri.getHost,
      "dataSource.portNumber"   → uri.getPort,
      "dataSource.databaseName" → dbName,
      "dataSource.user"         → sys.env(usernameHolder), //NoSuchElementFoundException can be thrown if no env variable is set
      "dataSource.password"     → sys.env(passwordHolder) //NoSuchElementFoundException can be thrown if no env variable is set
    )
    val dataSourceConfig = ConfigFactory.parseMap(dataSource.asJava)
    createDslInternal(Some(dataSourceConfig))
  }

  // throws DatabaseException
  def jCreateDsl(locationService: ILocationService, dbName: String): CompletableFuture[DSLContext] =
    createDsl(locationService.asScala, dbName).toJava.toCompletableFuture

  // throws DatabaseException
  def jCreateDsl(
      locationService: ILocationService,
      dbName: String,
      usernameHolder: String,
      passwordHolder: String
  ): CompletableFuture[DSLContext] =
    createDsl(locationService.asScala, dbName, usernameHolder, passwordHolder).toJava.toCompletableFuture

  // for dev/testing use picks details from config, throws DatabaseException
  def createDsl(): Future[DSLContext]             = Future(createDslInternal())
  def jCreateDsl(): CompletableFuture[DSLContext] = createDsl().toJava.toCompletableFuture

  private[database] def createDsl(port: Int): Future[DSLContext] = {
    val config = ConfigFactory.parseString(s"dataSource.portNumber = $port")
    Future(createDslInternal(Some(config)))
  }

  /************ INTERNAL ************/
  private def createDslInternal(maybeConfig: Option[Config] = None): DSLContext = {
    val cswDatabase: Config      = config.getConfig("csw-database")
    val dataSourceConfig: Config = cswDatabase.getConfig("hikari-datasource")

    val finalDataSourceConfig = maybeConfig match {
      case None               ⇒ dataSourceConfig
      case Some(customConfig) ⇒ customConfig.withFallback(dataSourceConfig)
    }

    try {
      val hikariConfig = new HikariConfig(toProperties(finalDataSourceConfig))
      val dialect      = cswDatabase.getString("databaseDialect")

      log.info(s"Connecting to database using config :[$finalDataSourceConfig]")
      DSL.using(new HikariDataSource(hikariConfig), SQLDialect.valueOf(dialect))
    } catch {
      case NonFatal(ex) ⇒
        val exception = DatabaseException(ex.getMessage, ex.getCause)
        log.error(exception.getMessage, ex = exception)
        throw exception
    }
  }

  private def toProperties(config: Config): Properties = {
    val properties = new Properties()
    config.entrySet.forEach { e =>
      properties.setProperty(e.getKey, config.getString(e.getKey))
    }
    properties
  }
}
