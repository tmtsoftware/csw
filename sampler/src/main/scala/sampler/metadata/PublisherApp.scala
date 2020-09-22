package sampler.metadata

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import scala.concurrent.duration.DurationInt

object PublisherApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val client: LocationService                     = HttpLocationServiceFactory.makeLocalClient
  private val service: EventService                       = new EventServiceFactory().make(client)

  val publisher = service.defaultPublisher

  // observe event
  Source
    .tick(0.seconds, 2.seconds, ())
    .runForeach(_ => publisher.publish(ObserveEvent(Prefix(ESW, "observe"), EventName("expstr"))))

  // 100Hz event
  Source
    .tick(0.seconds, 10.millis, ()) //100 Hz * how many? 10?
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(ESW, "filter"), EventName("wheel"))))

//  Source
//    .tick(0.seconds, 100.millis, ()) // 10 Hz* 100
//    .runForeach(_ => publisher.publish(SystemEvent(Prefix(TCS, "filter1"), EventName("wheel"))))
//
//  Source
//    .tick(0.seconds, 1000.millis, ()) //  1Hz * 1000
//    .runForeach(_ => publisher.publish(SystemEvent(Prefix(IRIS, "filter2"), EventName("wheel2"))))
//
//  // Many to Many and Sampler : 5K - 6K  events/seconds
//
//  Source
//    .tick(0.seconds, 400.millis, ())
//    .runForeach(_ => publisher.publish(SystemEvent(Prefix(WFOS, "filter3"), EventName("wheel3"))))

}
