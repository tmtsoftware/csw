/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.api.internal

import java.util.concurrent.CompletableFuture

import org.apache.pekko.Done
import csw.event.api.javadsl.IEventSubscription
import csw.event.api.scaladsl.EventSubscription

import scala.jdk.FutureConverters.*

private[event] object EventServiceExts {
  implicit class RichEventSubscription(val eventSubscription: EventSubscription) {
    def asJava: IEventSubscription = {
      new IEventSubscription {
        override def unsubscribe(): CompletableFuture[Done] = eventSubscription.unsubscribe().asJava.toCompletableFuture

        override def ready(): CompletableFuture[Done] = eventSubscription.ready().asJava.toCompletableFuture
      }
    }
  }
}
