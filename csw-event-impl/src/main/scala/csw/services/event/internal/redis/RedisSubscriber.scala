package csw.services.event.internal.redis

import akka.actor.typed.ActorRef
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Source}
import akka.{Done, NotUsed}
import csw.messages.events._
import csw.services.event.internal.pubsub.{EventSubscriberUtil, JEventSubscriber}
import csw.services.event.javadsl.IEventSubscriber
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class RedisSubscriber(
    redisURI: RedisURI,
    redisClient: RedisClient,
    eventSubscriberUtil: EventSubscriberUtil
)(implicit ec: ExecutionContext)
    extends EventSubscriber {

  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  private def reactiveConnectionF(): Future[RedisPubSubReactiveCommands[EventKey, Event]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala.map(_.reactive())

  private def patternBasedReactiveConnection(): Future[RedisPubSubReactiveCommands[String, Event]] =
    redisClient.connectPubSubAsync(PatternBasedEventServiceCodec, redisURI).toScala.map(_.reactive())

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val connectionF                                  = reactiveConnectionF()
    val latestEventStream: Source[Event, NotUsed]    = Source.fromFuture(get(eventKeys)).mapConcat(identity)
    val eventStreamF: Future[Source[Event, NotUsed]] = connectionF.flatMap(subscribe(eventKeys, _))
    val eventStream: Source[Event, Future[NotUsed]]  = Source.fromFutureSource(eventStreamF)

    latestEventStream
      .concatMat(eventStream)(Keep.right)
      .viaMat(KillSwitches.single)(Keep.both)
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case ((subscriptionF, killSwitch), terminationSignal) ⇒
          new EventSubscription {
            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(commands.unsubscribe(eventKeys.toSeq: _*).toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }

            override def ready(): Future[Done] = subscriptionF.map(_ ⇒ Done)
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
      .subscribeAsync(subscribe(eventKeys).via(eventSubscriberUtil.subscriptionModeStage(every, mode)), callback)

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    eventSubscriberUtil.subscribeCallback(subscribe(eventKeys), callback)

  override def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription =
    eventSubscriberUtil
      .subscribeCallback(subscribe(eventKeys).via(eventSubscriberUtil.subscriptionModeStage(every, mode)), callback)

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef))

  override def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef), every, mode)

  override def pSubscribe(keyPatterns: Set[String]): Source[Event, EventSubscription] = {
    val connectionF = patternBasedReactiveConnection()
    val eventStream: Source[Event, Future[NotUsed]] =
      Source.fromFutureSource(patternBasedReactiveConnection().flatMap(c ⇒ pSubscribe(keyPatterns, c)))

    eventStream
      .viaMat(KillSwitches.single)(Keep.both)
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case ((subscriptionF, killSwitch), terminationSignal) ⇒
          new EventSubscription {
            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(commands.punsubscribe(keyPatterns.toString()).toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }

            override def ready(): Future[Done] = subscriptionF.map(_ ⇒ Done)
          }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = Future.sequence(eventKeys.map(get))

  override def get(eventKey: EventKey): Future[Event] = async {
    val connection = await(asyncConnectionF)
    val event      = await(connection.get(eventKey).toScala)
    if (event == null) Event.invalidEvent(eventKey) else event
  }

  private def subscribe(
      eventKeys: Set[EventKey],
      reactiveCommands: RedisPubSubReactiveCommands[EventKey, Event]
  ): Future[Source[Event, NotUsed]] =
    reactiveCommands
      .subscribe(eventKeys.toSeq: _*)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(reactiveCommands.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage))

  private def pSubscribe(keyPatterns: Set[String], reactiveCommands: RedisPubSubReactiveCommands[String, Event]) =
    reactiveCommands
      .psubscribe(keyPatterns.toSeq: _*)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(reactiveCommands.observePatterns(OverflowStrategy.LATEST)).map(_.getMessage))
}
