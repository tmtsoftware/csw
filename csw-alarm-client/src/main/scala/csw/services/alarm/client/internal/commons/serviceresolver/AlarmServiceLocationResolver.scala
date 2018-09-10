package csw.services.alarm.client.internal.commons.serviceresolver

import java.net.URI

import csw.services.location.api.scaladsl.LocationService
import csw.services.alarm.client.internal.commons.AlarmServiceConnection

import scala.async.Async._
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides the connection information of `Alarm Service` by resolving the location through `Location Service`
 */
class AlarmServiceLocationResolver(locationService: LocationService)(implicit ec: ExecutionContext) extends AlarmServiceResolver {

  def uri(): Future[URI] = async {
    val location = await(locationService.resolve(AlarmServiceConnection.value, 5.seconds)).getOrElse(
      throw new RuntimeException(
        s"Alarm service connection=${AlarmServiceConnection.value.name} can not be resolved"
      )
    )
    location.uri
  }
}
