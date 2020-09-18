package csw.event.client.metadata

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS, WFOS}

import scala.concurrent.duration.DurationInt

object PublisherApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val client: LocationService                     = HttpLocationServiceFactory.makeLocalClient
  private val service: EventService                       = new EventServiceFactory().make(client)

  val publisher = service.defaultPublisher

  Source
    .tick(0.seconds, 7.millis, ())
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(ESW, "filter"), EventName("wheel"))))

  Source
    .tick(0.seconds, 200.millis, ())
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(TCS, "filter1"), EventName("wheel"))))

  Source
    .tick(0.seconds, 300.millis, ())
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(IRIS, "filter2"), EventName("wheel2"))))

  Source
    .tick(0.seconds, 400.millis, ())
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(WFOS, "filter3"), EventName("wheel3"))))

  Source
    .tick(0.seconds, 2.seconds, ())
    .runForeach(_ => publisher.publish(ObserveEvent(Prefix(ESW, "observe"), EventName("expstr"))))
}
