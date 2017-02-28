package csw.services.location.scaladsl

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source

import scala.concurrent.Future

trait TrackingResult {
  def unregister(): Future[Done]
  def stream: Source[Location, NotUsed]
}
