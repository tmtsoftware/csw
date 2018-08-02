package csw.services.alarm.client.internal.commons
import java.net.URI

import csw.services.location.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

object AlarmServiceLocationResolver {

  def resolveWith(locationService: LocationService)(implicit ec: ExecutionContext): Future[URI] = async {
    val location = await(locationService.resolve(AlarmServiceConnection.value, 5.seconds)).getOrElse(
      throw new RuntimeException(
        s"Alarm service connection=${AlarmServiceConnection.value.name} can not be resolved"
      )
    )
    location.uri
  }

  def resolveWith(host: String, port: Int): Future[URI] = Future.successful(new URI(s"tcp://$host:$port"))

}
