package csw.services.event.internal.redis

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, Materializer}
import akka.{Done, NotUsed}
import csw.messages.events._
import csw.messages.params.models.Subsystem
import csw.services.event.api.exceptions.EventServerNotAvailable
import csw.services.event.api.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw.services.event.internal.commons.EventSubscriberUtil
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.services.event.api.scaladsl.EventSubscriber]] API which uses Redis as the provider for publishing
 * and subscribing events.
 * @param redisURI Contains connection details for the Redis/Sentinel connections.
 * @param redisClient A redis client available from lettuce
 * @param ec the execution context to be used for performing asynchronous operations
 * @param mat the materializer to be used for materializing underlying streams
 */
class RedisSubscriber(redisURI: RedisURI, redisClient: RedisClient)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends EventSubscriber {

  private val eventSubscriberUtil = new EventSubscriberUtil()

  // create underlying connection asynchronously and obtain an instance of RedisAsyncCommands to perform
  // redis operations asynchronously. This instance of RedisAsyncCommands is used for performing `get`
  // operations on Redis asynchronously
  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    connection(redisClient.connectAsync(EventServiceCodec, redisURI).toScala).map(_.async())

  // create underlying connection asynchronously and obtain an instance of RedisPubSubReactiveCommands to perform
  // redis pub sub operations using a `reactor` based reactive API provided by lettuce Redis driver.
  private def reactiveConnectionF(): Future[RedisPubSubReactiveCommands[EventKey, Event]] =
    connection(redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala).map(_.reactive())

  // create a RedisPubSubReactiveCommands instance similar to above for pattern based subscription
  private def patternBasedReactiveConnection(): Future[RedisPubSubReactiveCommands[String, Event]] =
    connection(redisClient.connectPubSubAsync(PatternBasedEventServiceCodec, redisURI).toScala).map(_.reactive())

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val connectionF                                  = reactiveConnectionF()
    val latestEventStream: Source[Event, NotUsed]    = Source.fromFuture(get(eventKeys)).mapConcat(identity)
    val eventStreamF: Future[Source[Event, NotUsed]] = connectionF.flatMap(subscribe_internal(eventKeys, _))
    val eventStream: Source[Event, Future[NotUsed]]  = Source.fromFutureSource(eventStreamF)

    // get stream of latest events using the `get` API and concat it with the event stream from `subscribe_internal`
    latestEventStream
      .concatMat(eventStream)(Keep.right)
      .watchTermination()(Keep.both)
      .viaMat(KillSwitches.single)(Keep.both)
      .mapMaterializedValue {
        case ((subscriptionF, terminationSignal), killSwitch) ⇒
          new EventSubscription {

            terminationSignal.onComplete(_ => unsubscribe())

            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(commands.unsubscribe(eventKeys.toSeq: _*).toFuture.toScala)
              await(commands.quit().toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }

            override def ready(): Future[Done] = subscriptionF.map(_ ⇒ Done).recoverWith {
              case _ if terminationSignal.isCompleted ⇒ terminationSignal
            }
          }
      }
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
  ): EventSubscription =
    eventSubscriberUtil
      .subscribeAsync(subscribe(eventKeys, every, mode), callback)

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    eventSubscriberUtil.subscribeCallback(subscribe(eventKeys), callback)

  override def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription =
    eventSubscriberUtil
      .subscribeCallback(subscribe(eventKeys, every, mode), callback)

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef))

  override def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef), every, mode)

  override def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription] = {
    val connectionF = patternBasedReactiveConnection()
    val keyPattern  = s"${subsystem.entryName}.$pattern"
    val eventStream: Source[Event, Future[NotUsed]] =
      Source.fromFutureSource(patternBasedReactiveConnection().flatMap(c ⇒ pSubscribe_internal(keyPattern, c)))

    eventStream
      .watchTermination()(Keep.both)
      .viaMat(KillSwitches.single)(Keep.both)
      .mapMaterializedValue {
        case ((subscriptionF, terminationSignal), killSwitch) ⇒
          new EventSubscription {

            terminationSignal.onComplete(_ => unsubscribe())

            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(commands.punsubscribe(keyPattern.toString).toFuture.toScala)
              await(commands.quit().toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }

            override def ready(): Future[Done] = subscriptionF.map(_ ⇒ Done).recoverWith {
              case _ if terminationSignal.isCompleted ⇒ terminationSignal
            }
          }
      }
  }

  override def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription =
    eventSubscriberUtil.pSubscribe(pSubscribe(subsystem, pattern), callback)

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = Future.sequence(eventKeys.map(get))

  override def get(eventKey: EventKey): Future[Event] = async {
    val connection = await(asyncConnectionF)
    val event      = await(connection.get(eventKey).toScala)
    if (event == null) Event.invalidEvent(eventKey) else event
  }

  // get stream of events from redis `subscribe` command
  private def subscribe_internal(
      eventKeys: Set[EventKey],
      reactiveCommands: RedisPubSubReactiveCommands[EventKey, Event]
  ): Future[Source[Event, NotUsed]] =
    reactiveCommands
      .subscribe(eventKeys.toSeq: _*)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(reactiveCommands.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage))

  private def pSubscribe_internal(
      pattern: String,
      reactiveCommands: RedisPubSubReactiveCommands[String, Event]
  ): Future[Source[Event, NotUsed]] =
    reactiveCommands
      .psubscribe(pattern)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(reactiveCommands.observePatterns(OverflowStrategy.LATEST)).map(_.getMessage))

  private def connection[T](commands: ⇒ Future[T]): Future[T] =
    Future.unit.flatMap(_ ⇒ commands).recover { case NonFatal(ex) ⇒ throw EventServerNotAvailable(ex.getCause) }
}
