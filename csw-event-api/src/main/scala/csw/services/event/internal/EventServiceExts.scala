package csw.services.event.internal

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.services.event.javadsl.IEventSubscription
import csw.services.event.scaladsl.EventSubscription

import scala.compat.java8.FutureConverters.FutureOps

object EventServiceExts {
  implicit class RichEventSubscription(val eventSubscription: EventSubscription) {
    def asJava: IEventSubscription = {
      new IEventSubscription {
        override def unsubscribe(): CompletableFuture[Done] = eventSubscription.unsubscribe().toJava.toCompletableFuture

        override def ready(): CompletableFuture[Done] = eventSubscription.ready().toJava.toCompletableFuture
      }
    }
  }
}
