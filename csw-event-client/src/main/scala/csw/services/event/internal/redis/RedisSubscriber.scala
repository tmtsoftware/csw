package csw.services.event.internal.redis

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, Materializer}
import akka.{Done, NotUsed}
import csw.messages.events._
import csw.messages.params.models.Subsystem
import csw.services.event.api.exceptions.EventServerNotAvailable
import csw.services.event.api.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw.services.event.internal.commons.{EventServiceLogger, EventSubscriberUtil}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.RedisCodec
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

  private val log                 = EventServiceLogger.getLogger
  private val eventSubscriberUtil = new EventSubscriberUtil()

  // create underlying connection asynchronously and obtain an instance of RedisAsyncCommands to perform
  // redis operations asynchronously. This instance of RedisAsyncCommands is used for performing `get`
  // operations on Redis asynchronously
  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    connection(redisClient.connectAsync(EventServiceCodec, redisURI).toScala).map(_.async())

  // create underlying connection asynchronously and obtain an instance of RedisPubSubReactiveCommands to perform
  // redis pub sub operations using a `reactor` based reactive API provided by lettuce Redis driver.
  private def reactiveConnectionF[T](codec: RedisCodec[T, Event]): Future[RedisPubSubReactiveCommands[T, Event]] =
    connection(redisClient.connectPubSubAsync(codec, redisURI).toScala).map(_.reactive())

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    log.info(s"Subscribing to event keys: $eventKeys")
    val connectionF = reactiveConnectionF(EventServiceCodec)

    val latestEventStream: Source[Event, NotUsed]        = Source.fromFuture(get(eventKeys)).mapConcat(identity)
    val eventStream: Source[Event, Future[NotUsed]]      = Source.fromFutureSource(subscribe_internal(eventKeys, connectionF))
    val finalEventStream: Source[Event, Future[NotUsed]] = latestEventStream.concatMat(eventStream)(Keep.right)

    val unsubscribe = () ⇒
      connectionF.flatMap { commands ⇒
        log.info(s"Unsubscribing to event keys: $eventKeys")
        commands.unsubscribe(eventKeys.toSeq: _*).toFuture.toScala
    }

    subscription(finalEventStream, connectionF, unsubscribe)
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

    val connectionF = reactiveConnectionF(PatternBasedEventServiceCodec)
    val eventStream: Source[Event, Future[NotUsed]] =
      Source.fromFutureSource(pSubscribe_internal(keyPattern, connectionF))

    val unsubscribe = () ⇒
      connectionF.flatMap {
        log.info(s"Unsubscribing to event key pattern: $keyPattern")
        commands ⇒
          commands.punsubscribe(keyPattern).toFuture.toScala
    }

    subscription(eventStream, connectionF, unsubscribe)
  }

  override def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription =
    eventSubscriberUtil.pSubscribe(pSubscribe(subsystem, pattern), callback)

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = Future.sequence(eventKeys.map(get))

  override def get(eventKey: EventKey): Future[Event] = async {
    log.info(s"Fetching event key: $eventKey")
    val commands = await(asyncConnectionF)
    val event    = await(commands.get(eventKey).toScala)
    if (event == null) Event.invalidEvent(eventKey) else event
  }

  // get stream of events from redis `subscribe` command
  private def subscribe_internal(
      eventKeys: Set[EventKey],
      connectionF: Future[RedisPubSubReactiveCommands[EventKey, Event]]
  ): Future[Source[Event, NotUsed]] = async {
    val commands = await(connectionF)
    val eventStreamF: Future[Source[Event, NotUsed]] = commands
      .subscribe(eventKeys.toSeq: _*)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(commands.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage))

    await(eventStreamF)
  }

  // get stream of events from redis `psubscribe` command
  private def pSubscribe_internal(
      pattern: String,
      connectionF: Future[RedisPubSubReactiveCommands[String, Event]]
  ): Future[Source[Event, NotUsed]] = async {
    val commands = await(connectionF)
    val eventStream = commands
      .psubscribe(pattern)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(commands.observePatterns(OverflowStrategy.LATEST)).map(_.getMessage))
    await(eventStream)
  }

  private def subscription[T](
      eventStream: Source[Event, Future[NotUsed]],
      connectionF: Future[RedisPubSubReactiveCommands[T, Event]],
      unsubscribeBehavior: () ⇒ Future[Void]
  ): Source[Event, EventSubscription] =
    eventStream
      .watchTermination()(Keep.both)
      .viaMat(KillSwitches.single)(Keep.both)
      .mapMaterializedValue {
        case ((subscriptionF, terminationSignal), killSwitch) ⇒
          new EventSubscription {
            terminationSignal.onComplete(_ => unsubscribe()) //unsubscribe on stream termination

            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(unsubscribeBehavior())
              await(commands.quit().toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }

            override def ready(): Future[Done] = subscriptionF.map(_ ⇒ Done).recoverWith {
              case _ if terminationSignal.isCompleted ⇒ terminationSignal
            }
          }
      }

  private def connection[T](commands: ⇒ Future[T]): Future[T] =
    Future.unit.flatMap(_ ⇒ commands).recover { case NonFatal(ex) ⇒ throw EventServerNotAvailable(ex.getCause) }
}
