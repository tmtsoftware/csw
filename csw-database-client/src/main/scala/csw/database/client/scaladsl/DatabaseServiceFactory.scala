package csw.database.client.scaladsl

import java.sql.DriverManager

import csw.database.api.javadasl.IDatabaseService
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.commons.serviceresolver.{DatabaseServiceHostPortResolver, DatabaseServiceLocationResolver}
import csw.database.client.internal.{DatabaseServiceImpl, JDatabaseServiceImpl}
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService

import scala.concurrent.ExecutionContext

class DatabaseServiceFactory {

  def make(locationService: LocationService, dbName: String)(implicit ec: ExecutionContext): DatabaseService = {
    val locationResolver = new DatabaseServiceLocationResolver(locationService)
    val connectionF = locationResolver.uri().map { uri =>
      DriverManager.getConnection(s"jdbc:postgresql://${uri.getHost}:${uri.getPort}/$dbName?user=postgres&password=postgres")
    }
    new DatabaseServiceImpl(connectionF)
  }

  def jMake(locationService: ILocationService, dbName: String, ec: ExecutionContext): IDatabaseService =
    new JDatabaseServiceImpl(make(locationService.asScala, dbName)(ec))

  def make(host: String, port: Int, dbName: String)(implicit ec: ExecutionContext): DatabaseService = {
    val hostPortResolver = new DatabaseServiceHostPortResolver(host, port)
    val connectionF = hostPortResolver.uri().map { uri =>
      DriverManager.getConnection(s"jdbc:postgresql://${uri.getHost}:${uri.getPort}/$dbName?user=postgres&password=postgres")
    }
    new DatabaseServiceImpl(connectionF)
  }

  def jMake(host: String, port: Int, dbName: String, ec: ExecutionContext): IDatabaseService =
    new JDatabaseServiceImpl(make(host, port, dbName)(ec))
}
