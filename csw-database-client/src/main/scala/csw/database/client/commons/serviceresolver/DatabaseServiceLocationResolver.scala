package csw.database.client.commons.serviceresolver

import java.net.URI

import csw.database.client.commons.DatabaseServiceConnection
import csw.location.api.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides the connection information of `Database Service` by resolving the location through `Database Service`
 */
class DatabaseServiceLocationResolver(locationService: LocationService)(implicit ec: ExecutionContext)
    extends DatabaseServiceResolver {

  def uri(): Future[URI] = async {
    val location = await(locationService.resolve(DatabaseServiceConnection.value, 5.seconds)).getOrElse(
      throw new RuntimeException(
        s"Database service connection=${DatabaseServiceConnection.value.name} can not be resolved"
      )
    )
    location.uri
  }
}
