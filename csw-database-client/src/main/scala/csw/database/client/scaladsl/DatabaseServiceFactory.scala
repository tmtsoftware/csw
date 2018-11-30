package csw.database.client.scaladsl
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.commons.serviceresolver.{DatabaseServiceHostPortResolver, DatabaseServiceLocationResolver}
import csw.database.client.internal.DatabaseServiceImpl
import csw.location.api.scaladsl.LocationService
import slick.jdbc.PostgresProfile.api.Database

import scala.concurrent.{ExecutionContext, Future}

class DatabaseServiceFactory {

  def make(locationService: LocationService, dbName: String, user: String)(implicit ec: ExecutionContext): DatabaseService = {
    val locationResolver = new DatabaseServiceLocationResolver(locationService)
    val connectionF = locationResolver.uri().map { uri =>
      val url = s"jdbc:postgresql://${uri.getHost}:${uri.getPort}/$dbName?user=$user"
      Database.forConfig("", getConfigWithUrl(url))
    }
    new DatabaseServiceImpl(connectionF)
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

  def make(configPath: String)(implicit ec: ExecutionContext): DatabaseService =
    new DatabaseServiceImpl(Future(Database.forConfig(configPath)))

  private def getConfigWithUrl(url: String): Config = {
    ConfigFactory
      .load()
      .getConfig("postgresConfig")
      .withValue("url", ConfigValueFactory.fromAnyRef(url))
  }

}
