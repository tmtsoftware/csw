package sampler.metadata

import java.time.Duration

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.client.internal.redis.RedisSubscriber
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}
import sampler.metadata.SamplerUtil.{printAggregates, recordHistogram}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MetadataGetSampler(redisSubscriber: RedisSubscriber)(implicit actorSystem: ActorSystem[_]) {

  val eventTimeDiffList: ListBuffer[Long] = scala.collection.mutable.ListBuffer()
  val snapshotTimeList: ListBuffer[Long]  = scala.collection.mutable.ListBuffer()

  def snapshot(obsEvent: Event): Unit = {
    val startTime = System.currentTimeMillis()

    val pattern      = EventKey("*.*.*")
    val eventKeys    = Await.result(redisSubscriber.keys(pattern), 10.seconds).toSet
    val lastSnapshot = Await.result(redisSubscriber.get(eventKeys), 10.seconds)
    val endTime      = System.currentTimeMillis()

    val event: Event        = lastSnapshot.find(_.eventKey.equals(EventKey("ESW.filter.wheel"))).getOrElse(Event.badEvent())
    val eventTimeDiff: Long = Duration.between(event.eventTime.value, obsEvent.eventTime.value).toMillis
    val snapshotTime: Long  = endTime - startTime

    //Event time diff
    val eventDiffNormalized = if (snapshotTime < 0) Math.abs(eventTimeDiff) + 10 else eventTimeDiff
    eventTimeDiffList.addOne(eventDiffNormalized)

    //Snapshot time diff
    snapshotTimeList.addOne(snapshotTime)
    recordHistogram(Math.abs(eventDiffNormalized), Math.abs(snapshotTime))

    println(s"${lastSnapshot.size} ,$eventDiffNormalized ,$snapshotTime")
  }

  def subscribeObserveEvents(): Future[Done] = {
    val subscriberSource = redisSubscriber.subscribe(Set(EventKey("esw.observe.expstr"))).drop(50).take(1000).runForeach(snapshot)
    subscriberSource
  }

  def start(): Future[Done] = {
    //print aggregates
    CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "print aggregates") { () =>
      printAggregates(eventTimeDiffList, snapshotTimeList)
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

  Await.result(sampler.start(), 20.minutes)
  system.terminate()
  redisClient.shutdown()
}
