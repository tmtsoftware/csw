package sampler.metadata

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import csw.event.client.internal.redis.RedisPublisher
import csw.location.client.ActorSystemFactory
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object PublisherApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val redisClient: RedisClient                    = RedisClient.create()
  private val eventualRedisURI: Future[RedisURI] =
    Future.successful(RedisURI.Builder.sentinel("localhost", 26379, "eventServer").build())

  private val publisher = new RedisPublisher(eventualRedisURI, redisClient)
  private var counter   = 1
  // observe event
  private val observeEventSource: Future[Done] = Source
    .repeat("abc")
    .throttle(1, 1.seconds)
    .take(1200)
    .runForeach(_ => {
      println(s"Publishing observe event $counter")
      counter += 1
      publisher.publish(ObserveEvent(Prefix(ESW, "observe"), EventName("expstr")))
    })

  // 100Hz event
  Source
    .tick(0.seconds, 10.millis, ()) //100 Hz * how many? 10?
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(ESW, "filter"), EventName("wheel"))))

  Await.result(observeEventSource, 18.minutes)

  redisClient.shutdown()
  system.terminate()

}
