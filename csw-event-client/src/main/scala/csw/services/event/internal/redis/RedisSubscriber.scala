package csw.services.event.internal.redis

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Source}
import akka.{Done, NotUsed}
import csw.messages.events._
import csw.messages.params.models.Subsystem
import csw.services.event.api.exceptions.EventServerNotAvailable
import csw.services.event.api.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw.services.event.internal.commons.{EventServiceLogger, EventSubscriberUtil}
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.exceptions.RedisServerNotAvailable
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}

import scala.async.Async._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.services.event.api.scaladsl.EventSubscriber]] API which uses Redis as the provider for publishing
 * and subscribing events.
 *
 * @param redisURI    contains connection details for the Redis/Sentinel connections.
 * @param redisClient redis client available from lettuce
 * @param ec          the execution context to be used for performing asynchronous operations
 * @param mat         the materializer to be used for materializing underlying streams
 */
class RedisSubscriber(redisURI: RedisURI, redisClient: RedisClient)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends EventSubscriber {

  import EventRomaineCodecs._

  private val log                 = EventServiceLogger.getLogger
  private val eventSubscriberUtil = new EventSubscriberUtil()

  private val romaineFactory = new RomaineFactory(redisClient)

  // create underlying connection asynchronously and obtain an instance of RedisAsyncCommands to perform
  // redis operations asynchronously. This instance of RedisAsyncCommands is used for performing `get`
  // operations on Redis asynchronously
  private lazy val asyncConnectionF: Future[RedisAsyncApi[EventKey, Event]] =
    connection(romaineFactory.redisAsyncApi[EventKey, Event](redisURI))

  // create underlying connection asynchronously and obtain an instance of RedisPubSubReactiveCommands to perform
  // redis pub sub operations using a `reactor` based reactive API provided by lettuce Redis driver.
  private def reactiveConnectionF[T: RomaineStringCodec](): RedisSubscriptionApi[T, Event] =
    romaineFactory.redisSubscriptionApi[T, Event](Future.successful(redisURI))

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    log.info(s"Subscribing to event keys: $eventKeys")
    val subscriptionApi: RedisSubscriptionApi[EventKey, Event] = reactiveConnectionF()

    val latestEventStream: Source[Event, NotUsed] = Source.fromFuture(get(eventKeys)).mapConcat(identity)
    val redisStream: Source[Event, RedisSubscription] =
      subscriptionApi.subscribe(eventKeys.toList, OverflowStrategy.LATEST).map(_.value)
    val eventStream: Source[Event, EventSubscription] = subscribeInternal(eventKeys, redisStream)
    latestEventStream.concatMat(eventStream)(Keep.right)
  }

  override def subscribe(
      eventKeys: Set[EventKey],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): Source[Event, EventSubscription] = subscribe(eventKeys).via(eventSubscriberUtil.subscriptionModeStage(every, mode))

  override def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription =
    eventSubscriberUtil.subscribeAsync(subscribe(eventKeys), callback)

  override def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = eventSubscriberUtil.subscribeAsync(subscribe(eventKeys, every, mode), callback)

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    eventSubscriberUtil.subscribeCallback(subscribe(eventKeys), callback)

  override def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = eventSubscriberUtil.subscribeCallback(subscribe(eventKeys, every, mode), callback)

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef))

  override def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef), every, mode)

  override def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription] = {
    val keyPattern = s"${subsystem.entryName}.$pattern"
    log.info(s"Subscribing to event key pattern: $keyPattern")

    val subscriptionApi: RedisSubscriptionApi[String, Event] = reactiveConnectionF()
    val redisStream: Source[Event, RedisSubscription] =
      subscriptionApi.psubscribe(List(keyPattern), OverflowStrategy.LATEST).map(_.value)
    subscribeInternal(keyPattern, redisStream)
  }

  override def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription =
    eventSubscriberUtil.pSubscribe(pSubscribe(subsystem, pattern), callback)

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = Future.sequence(eventKeys.map(get))

  override def get(eventKey: EventKey): Future[Event] = async {
    log.info(s"Fetching event key: $eventKey")
    val commands = await(asyncConnectionF)
    val event    = await(commands.get(eventKey))
    event.getOrElse(Event.invalidEvent(eventKey))
  }

  // get stream of events from redis `subscribe` command
  private def subscribeInternal[T](
      eventKeys: T,
      eventStreamF: Source[Event, RedisSubscription]
  ): Source[Event, EventSubscription] = {
    eventStreamF.mapMaterializedValue { subscriptionF =>
      new EventSubscription {
        override def unsubscribe(): Future[Done] = {
          log.info(s"Unsubscribing for keys=$eventKeys")
          subscriptionF.unsubscribe()
        }
        override def ready(): Future[Done] = subscriptionF.ready().recover {
          case RedisServerNotAvailable(ex) => throw EventServerNotAvailable(ex)
        }
      }
    }
  }

  private def connection[T](commands: ⇒ Future[T]): Future[T] =
    Future.unit.flatMap(_ ⇒ commands).recover { case NonFatal(ex) ⇒ throw EventServerNotAvailable(ex.getCause) }
}
