/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, typed}
import csw.event.client.EventServiceFactory
import csw.event.client.helpers.Utils
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.Event
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class InitialPublishLatencyTest extends AnyFunSuite with BeforeAndAfterAll {

  private implicit val system: ActorSystem               = ActorSystem()
  private implicit val typedSystem: typed.ActorSystem[_] = system.toTyped
  private val ls: LocationService                        = HttpLocationServiceFactory.makeLocalClient
  private val eventServiceFactory                        = new EventServiceFactory().make(ls)
  import eventServiceFactory._

  ignore("should not incurr high latencies for initially published events") {
    val event = Utils.makeEvent(0)

    defaultSubscriber.subscribeCallback(Set(event.eventKey), report)

    (0 to 500).foreach { id =>
      defaultPublisher.publish(Utils.makeEvent(id))
      Thread.sleep(10)
    }

    def report(event: Event): Unit = println(System.currentTimeMillis() - event.eventTime.value.toEpochMilli)
  }

}
