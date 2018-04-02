package csw.services.event.internal.redis

import java.util
import java.util.concurrent.CompletableFuture

import akka.Done
import akka.stream.javadsl.Source
import csw.messages.events.{Event, EventKey}
import csw.services.event.javadsl.{IEventSubscriber, IEventSubscription}
import csw.services.event.scaladsl.EventSubscriber

import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, setAsJavaSetConverter}
import scala.compat.java8.FutureConverters.FutureOps

class JRedisSubscriber(redisSubscriber: EventSubscriber) extends IEventSubscriber {

  override def subscribe(eventKeys: util.Set[EventKey]): Source[Event, IEventSubscription] =
    redisSubscriber.subscribe(eventKeys.asScala.toSet).asJava.mapMaterializedValue { eventSubscription ⇒
      new IEventSubscription {
        override def unsubscribe(): CompletableFuture[Done] = eventSubscription.unsubscribe().toJava.toCompletableFuture
      }
    }

  override def get(eventKeys: util.Set[EventKey]): CompletableFuture[util.Set[Event]] =
    redisSubscriber.get(eventKeys.asScala.toSet).toJava.toCompletableFuture.thenApply(_.asJava)

  override def get(eventKey: EventKey): CompletableFuture[Event] = redisSubscriber.get(eventKey).toJava.toCompletableFuture
}
