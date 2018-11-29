package csw.database.client.scaladsl
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import csw.database.api.javadasl.IDatabaseService
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.commons.serviceresolver.{DatabaseServiceHostPortResolver, DatabaseServiceLocationResolver}
import csw.database.client.internal.{DatabaseServiceImpl, JDatabaseServiceImpl}
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class DatabaseServiceFactory {
  def getConfigWithUrl(url: String): Config = {
    ConfigFactory
      .load()
      .getConfig("postgresConfig")
      .withValue("url", ConfigValueFactory.fromAnyRef(url))
  }

  def make(locationService: LocationService, dbName: String, user: String)(implicit ec: ExecutionContext): DatabaseService = {
    val locationResolver = new DatabaseServiceLocationResolver(locationService)
    val connectionF = locationResolver.uri().map { uri =>
      val url = s"jdbc:postgresql://${uri.getHost}:${uri.getPort}/$dbName?user=$user"
      Database.forConfig("", getConfigWithUrl(url))
    }
    new DatabaseServiceImpl(connectionF)
  }

  def jMake(locationService: ILocationService,
            dbName: String,
            user: String,
            executionContext: ExecutionContext): IDatabaseService = {
    implicit val ec: ExecutionContext = executionContext
    new JDatabaseServiceImpl(make(locationService.asScala, dbName, user))
  }

  def make(host: String, port: Int, dbName: String, user: String)(implicit ec: ExecutionContext): DatabaseService = {
    val hostPortResolver = new DatabaseServiceHostPortResolver(host, port)
    val connectionF = hostPortResolver.uri().map { uri =>
      val url    = s"jdbc:postgresql://${uri.getHost}:${uri.getPort}/$dbName?user=$user"
      val config = getConfigWithUrl(url)
      Database.forConfig("", config)
    }
    new DatabaseServiceImpl(connectionF)
  }

  def jMake(host: String, port: Int, dbName: String, user: String, executionContext: ExecutionContext): IDatabaseService = {
    implicit val ec: ExecutionContext = executionContext
    new JDatabaseServiceImpl(make(host, port, dbName, user))
  }

  def make(path: String)(implicit ec: ExecutionContext): DatabaseService =
    new DatabaseServiceImpl(Future(Database.forConfig(path)))

  def jMake(path: String, executionContext: ExecutionContext): IDatabaseService = {
    implicit val ec: ExecutionContext = executionContext
    new JDatabaseServiceImpl(make(path))
  }

}
