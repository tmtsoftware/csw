package sampler.metadata

import java.time.Duration

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.client.internal.redis.RedisSubscriber
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MetadataGetSampler(redisSubscriber: RedisSubscriber)(implicit actorSystem: ActorSystem[_]) {

  var sumOfDiffs        = 0L
  var numberOfSnapshots = 0d

  def snapshot(obsEvent: Event): Unit = {
    val pattern        = EventKey("*.*.*")
    val eventKeys      = Await.result(redisSubscriber.keys(pattern), 10.seconds).toSet
    val eventsSnapshot = Await.result(redisSubscriber.get(eventKeys), 10.seconds)

    val event = eventsSnapshot.find(_.eventKey.equals(EventKey("ESW.filter.wheel"))).getOrElse(Event.badEvent())
    val diff  = Duration.between(event.eventTime.value, obsEvent.eventTime.value).toMillis

    numberOfSnapshots += 1
    if (numberOfSnapshots > 10) { // to discard first values
      sumOfDiffs += Math.abs(diff)
    }

    print(diff + ",")
  }

  def subscribeObserveEvents(): Future[Done] =
    redisSubscriber.subscribe(Set(EventKey("esw.observe.expstr"))).runForeach(snapshot)

  def start(): Future[Done] = {
    //print aggregates
    CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "print aggregates") { () =>
      println("\n++++++++++++++++++++++++++++++++++")
      println("aggregated time diff " + (sumOfDiffs / (numberOfSnapshots - 10))) // - 10 for discard first value
      println("+++++++++++++++++++++++++++++++++")
      Future.successful(Done)
    }

    subscribeObserveEvents() // fire in background
  }
}

object MetadataGetSampler extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "get-sampler")

  private val redisClient: RedisClient = RedisClient.create()

  private val eventualRedisURI: Future[RedisURI] =
    Future.successful(RedisURI.Builder.sentinel("localhost", 26379, "eventServer").build())

  private val subscriber = new RedisSubscriber(eventualRedisURI, redisClient)

  private val sampler = new MetadataGetSampler(subscriber)

  Await.result(sampler.start(), 1000.seconds)
}
