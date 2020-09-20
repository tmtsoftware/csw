package csw.event.client.metadata

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.client.internal.redis.RedisSubscriber
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MetadataGetSampler(redisSubscriber: RedisSubscriber)(implicit actorSystem: ActorSystem[_]) {

  var queue: List[ConcurrentHashMap[EventKey, Event]] = List.empty // snapshot store

  val obsEventsToSnapshotOn: List[EventKey] = List.empty

  def snapshot(observeEvent: Event): Unit = {

    val pattern   = EventKey("*.*.*")
    val eventKeys = Await.result(redisSubscriber.keys(pattern), 5.seconds).toSet

    val eventsSnapshot = Await.result(redisSubscriber.get(eventKeys), 5.seconds)

    println("+=======================================")
    val sampledEvent = eventsSnapshot.find(_.eventKey.equals(EventKey("ESW.filter.wheel"))).getOrElse(Event.badEvent())
    println("time diff :" + Duration.between(sampledEvent.eventTime.value, observeEvent.eventTime.value).toMillis)
    println("number of events" + eventsSnapshot.size)
    println("+=======================================")
  }

  def subscribeObserveEvents(): Future[Done] =
    redisSubscriber.subscribe(Set(EventKey("esw.observe.expstr"))).runForeach(snapshot)

  def start(): Future[Done] = {
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
