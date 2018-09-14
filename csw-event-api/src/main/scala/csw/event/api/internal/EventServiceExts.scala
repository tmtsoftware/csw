package csw.event.api.internal

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.event.api.javadsl.IEventSubscription
import csw.event.api.scaladsl.EventSubscription

import scala.compat.java8.FutureConverters.FutureOps

private[event] object EventServiceExts {
  implicit class RichEventSubscription(val eventSubscription: EventSubscription) {
    def asJava: IEventSubscription = {
      new IEventSubscription {
        override def unsubscribe(): CompletableFuture[Done] = eventSubscription.unsubscribe().toJava.toCompletableFuture

        override def ready(): CompletableFuture[Done] = eventSubscription.ready().toJava.toCompletableFuture
      }
    }
  }
}
