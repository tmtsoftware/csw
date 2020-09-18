package csw.event.client.metadata

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.perf.utils.EventUtils
import csw.location.client.ActorSystemFactory
import csw.params.events.{EventKey, EventName}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object PublisherApp2 extends App {

  private implicit val actorSystem: ActorSystem[_] = {
    ActorSystemFactory.remote(SpawnProtocol(), "app-actor-system")
  }
  import actorSystem.executionContext

  //-----------CREATE PAYLOAD AND KEYS
  val eventService1: EventService = new EventServiceFactory().make("localhost", 26379)
  val publisher                   = eventService1.defaultPublisher
  val prefix                      = Prefix(ESW, "filter56")
  val keys                        = (1 to 1000).map(i => EventKey(prefix, EventName(s"EventKey$i"))).toSet
  val payloadSize                 = 1024
  val payload: Array[Byte]        = ("0" * payloadSize).getBytes("utf-8") //  println("Payload : " + payload.toList)

  //-----------PUBLISH EVENTS
  private val before: Long = System.currentTimeMillis()
  println("before publish= " + before)

  for (a <- 1 to 100) {
    val future = Future.traverse(keys) { key =>
      val systemEvent = EventUtils.event(key.eventName, key.source, payload = payload)
      publisher.publish(systemEvent)
    }
    Await.result(future, 10.seconds)
    println("rounds published : " + a)

    Thread.sleep(500)
  }
}
