package csw.event.client.metadata

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.{Keep, Sink}
import csw.event.api.scaladsl.EventSubscriber
import csw.event.client.internal.redis.{RedisGlobalSubscriber, RedisSubscriber}
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

class MetadataPSubscribeSampler(
    globalSubscriber: RedisGlobalSubscriber,
    eventSubscriber: EventSubscriber
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  var currentState                                    = new ConcurrentHashMap[EventKey, Event]() // mutable map with variable ref
  var queue: List[ConcurrentHashMap[EventKey, Event]] = List.empty                               // snapshot store

  val obsEventsToSnapshotOn: List[EventKey] = List.empty

  def snapshot(e: Event): Unit = {
    val ss = currentState
    currentState = new ConcurrentHashMap() //change it with new Map
    queue = ss :: queue                    // data store

    println("+=======================================")
    val event = ss.getOrDefault(EventKey("ESW.filter.wheel"), Event.badEvent())
    println("time diff :" + Duration.between(event.eventTime.value, e.eventTime.value).toMillis)
    println("number of keys" + ss.keys().asScala.toList.size)
    println("+=======================================")
  }

  def subscribeObserveEvents(): Future[Done] =
    eventSubscriber.subscribe(Set(EventKey("esw.observe.expstr"))).runForeach(snapshot)

  def start(): Future[Done] = {
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
