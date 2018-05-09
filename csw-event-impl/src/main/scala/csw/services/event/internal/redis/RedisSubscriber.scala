package csw.services.event.internal.redis

import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, Materializer}
import akka.{Done, NotUsed}
import csw.messages.events._
import csw.services.event.internal.pubsub.{AbstractEventSubscriber, JAbstractEventSubscriber}
import csw.services.event.javadsl.IEventSubscriber
import csw.services.event.scaladsl.EventSubscription
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisSubscriber(redisURI: RedisURI, redisClient: RedisClient)(
    implicit ec: ExecutionContext,
    protected val mat: Materializer
) extends AbstractEventSubscriber {

  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  private def reactiveConnectionF(): Future[RedisPubSubReactiveCommands[EventKey, Event]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala.map(_.reactive())

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

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = Future.sequence(eventKeys.map(get))

  override def get(eventKey: EventKey): Future[Event] = async {
    val connection = await(asyncConnectionF)
    val event      = await(connection.get(eventKey).toScala)
    if (event == null) Event.invalidEvent(eventKey) else event
  }

  override def asJava: IEventSubscriber = new JAbstractEventSubscriber(this)

  private def subscribe(
      eventKeys: Set[EventKey],
      reactiveCommands: RedisPubSubReactiveCommands[EventKey, Event]
  ): Future[Source[Event, NotUsed]] =
    reactiveCommands
      .subscribe(eventKeys.toSeq: _*)
      .toFuture
      .toScala
      .map(_ ⇒ Source.fromPublisher(reactiveCommands.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage))
}
