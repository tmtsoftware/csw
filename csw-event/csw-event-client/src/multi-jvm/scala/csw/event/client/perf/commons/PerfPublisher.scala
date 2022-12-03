/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.commons

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Source}
import csw.event.api.scaladsl.EventPublisher
import csw.event.client.perf.utils.EventUtils.*
import csw.event.client.perf.wiring.{TestConfigs, TestWiring}
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, DurationLong}

class PerfPublisher(
    prefix: Prefix,
    pubId: Int,
    eventsSetting: EventsSetting,
    testConfigs: TestConfigs,
    testWiring: TestWiring,
    sharedPublisher: EventPublisher
) {
  import eventsSetting._
  import testConfigs._
  import testWiring._

  private val totalMessages        = totalTestMsgs + warmup + 1 //inclusive of end-event
  private val payload: Array[Byte] = ("0" * payloadSize).getBytes("utf-8")

  private val publisher: EventPublisher =
    if (shareConnection) sharedPublisher else testWiring.publisher

  private val eventName                = EventName(s"$testEventS-$pubId")
  private val endEventName             = EventName(s"$endEventS-$pubId")
  private val endEvent                 = event(endEventName, prefix)
  private var eventId                  = 0
  private var cancellable: Cancellable = _
  private var remaining: Long          = totalMessages

  private def eventGenerator(): Option[SystemEvent] = Option {
    eventId += 1
    // send extra two end events in case one goes missing
    // subscriber stops listening on receiving first end event, hence not affected by publisher publishing multiple end events
    if (eventId > totalMessages + 10) cancellable.cancel()
    if (eventId < totalMessages) event(eventName, prefix, eventId, payload) else endEvent
  }

  private def source(eventName: EventName): Source[SystemEvent, Future[Done]] =
    Source(1L to totalMessages)
      .map { id =>
        event(eventName, prefix, id, payload)
      }
      .watchTermination()(Keep.right)

  def startPublishingWithEventGenerator(): Unit = { cancellable = publisher.publish(eventGenerator(), (1000 / rate).millis) }

  def startPublishingWithSource(): Future[Done] =
    for {
      _   <- publisher.publish(source(eventName))
      end <- publisher.publish(endEvent)
    } yield end

  def startPublishingInBatches(): Future[Done] = async {

    while (eventId < totalMessages) {
      val batchSize = Math.min(burstSize, remaining)
      var i         = 0

      var batchCompletionF: List[Future[Done]] = Nil

      while (i < batchSize) {
        batchCompletionF = publisher.publish(event(eventName, prefix, eventId, payload)) :: batchCompletionF
        eventId += 1
        i += 1
      }
      remaining = remaining - batchSize
      await(Future.sequence(batchCompletionF))
    }

    await(publisher.publish(event(eventName, prefix)))
  }

}
