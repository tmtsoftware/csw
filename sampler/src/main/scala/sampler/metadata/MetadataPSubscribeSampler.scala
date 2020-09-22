package sampler.metadata

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.{Keep, Sink}
import csw.event.api.scaladsl.EventSubscriber
import csw.event.client.internal.redis.{RedisGlobalSubscriber, RedisSubscriber}
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}
import sampler.metadata.MetadataPSubscribeSampler.system

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MetadataPSubscribeSampler(
    globalSubscriber: RedisGlobalSubscriber,
    eventSubscriber: EventSubscriber
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  var currentState = new ConcurrentHashMap[EventKey, Event]() // mutable map with variable ref
  var lastSnapshot = new ConcurrentHashMap[EventKey, Event]()

  var sumOfDiffs        = 0L
  var numberOfSnapshots = 0d

  def snapshot(obsEvent: Event): Unit = {
    lastSnapshot = currentState
    currentState = new ConcurrentHashMap(lastSnapshot) //change it with new Map and clone previous snapshot.

    val event = lastSnapshot.getOrDefault(EventKey("ESW.filter.wheel"), Event.badEvent())
    val diff  = Duration.between(event.eventTime.value, obsEvent.eventTime.value).toMillis

    numberOfSnapshots += 1
    if (numberOfSnapshots > 10) { // to discard first values
      sumOfDiffs += diff
    }
    print(diff + ",")
  }

  def subscribeObserveEvents(): Future[Done] =
    eventSubscriber.subscribe(Set(EventKey("esw.observe.expstr"))).runForeach(snapshot)

  def start(): Future[Done] = {
    //print aggregates
    CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "print aggregates") { () =>
      println("\n++++++++++++++++++++++++++++++++++")
      println("aggregated time diff " + (sumOfDiffs / (numberOfSnapshots - 10))) // - 10 for discard first value
      println("+++++++++++++++++++++++++++++++++")
      Future.successful(Done)
    }

    subscribeObserveEvents() // fire in background

    globalSubscriber
      .subscribeAll()
      .toMat(Sink.foreachAsync(4) { e => // todo: think about this
        Future.unit.map { _ => currentState.put(e.eventKey, e) }
      })(Keep.right)
      .run()
  }
}

object MetadataPSubscribeSampler extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private val redisClient: RedisClient = RedisClient.create()

  private val eventualRedisURI: Future[RedisURI] =
    Future.successful(RedisURI.Builder.sentinel("localhost", 26379, "eventServer").build())

  private val subscriber = new RedisSubscriber(eventualRedisURI, redisClient)

  private val globalSubscriber: RedisGlobalSubscriber = RedisGlobalSubscriber.make(redisClient, eventualRedisURI)

  private val sampler = new MetadataPSubscribeSampler(globalSubscriber, subscriber)

  Await.result(sampler.start(), 1000.seconds)
}
