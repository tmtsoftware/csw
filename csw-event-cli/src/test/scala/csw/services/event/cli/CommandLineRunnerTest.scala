package csw.services.event.cli

import akka.stream.scaladsl.Sink
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.makeEventForKeyName
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.collection.{immutable, mutable}
import scala.concurrent.Await
import csw.services.event.cli.BufferExtensions.RichBuffer
import scala.io.Source

class CommandLineRunnerTest extends FunSuite with Matchers with SeedData with Eventually {

  def events(name: EventName): immutable.Seq[Event] = for (i ← 1 to 1500) yield makeEventForKeyName(name, i)

  class EventGenerator(eventName: EventName) {
    var counter                               = 0
    var publishedEvents: mutable.Queue[Event] = mutable.Queue.empty
    val eventsGroup: immutable.Seq[Event]     = events(eventName)

    def generate: Event = {
      val event = eventsGroup(counter)
      counter += 1
      publishedEvents.enqueue(event)
      event
    }
  }

  import cliWiring._

  // DEOPSCSW-364: [Event Cli] Inspect command
  test("should able to inspect event/events containing multiple parameters including recursive structs") {

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "-e", s"${event1.eventKey}")).get).await
    logBuffer shouldEqualContentsOf "inspect/expected/event1.txt"

    logBuffer.clear()

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "--events", s"${event2.eventKey}")).get).await
    logBuffer shouldEqualContentsOf "inspect/expected/event2.txt"

    logBuffer.clear()

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "-e", s"${event1.eventKey},${event2.eventKey}")).get).await
    logBuffer shouldEqualContentsOf "inspect/expected/event1And2.txt"
  }

  // DEOPSCSW-431: [Event Cli] Get command
  test("should able to get entire event/events in json format") {

    commandLineRunner.get(argsParser.parse(Seq("get", "-e", s"${event1.eventKey}", "-o", "json")).get).await
    stringToEvent(logBuffer.head) shouldBe event1

    logBuffer.clear()

    commandLineRunner.get(argsParser.parse(Seq("get", "-e", s"${event1.eventKey},${event2.eventKey}", "--out", "json")).get).await
    val events = logBuffer.map(stringToEvent).toSet
    events shouldEqual Set(event1, event2)
  }

  // DEOPSCSW-432: [Event Cli] Publish command
  test("should able to publish event without event key provided") {
    val path              = getClass.getResource("/publish/observe_event.json").getPath
    val expectedEventJson = Json.parse(Source.fromResource("publish/observe_event.json").mkString)

    // observe_event.json file contains this event key
    val eventKey = "wfos.blue.filter.filter_wheel"

    commandLineRunner.publish(argsParser.parse(Seq("publish", "--data", path)).get).await

    eventually(timeout = timeout(5.seconds), interval = interval(100.millis)) {
      commandLineRunner.get(argsParser.parse(Seq("get", "-e", eventKey, "-o", "json")).get).await

      removeDynamicKeys(Json.parse(logBuffer.last)) shouldBe removeDynamicKeys(expectedEventJson)
    }
  }

  // DEOPSCSW-432: [Event Cli] Publish command
  test("should able to publish event when event key and event json file provided") {
    val path      = getClass.getResource("/publish/observe_event.json").getPath
    val eventJson = Json.parse(Source.fromResource("publish/observe_event.json").mkString)

    val eventKey = EventKey("wfos.blue.filter.wheel")
    commandLineRunner.publish(argsParser.parse(Seq("publish", "-e", s"${eventKey.key}", "--data", path)).get).await

    eventually(timeout = timeout(5.seconds), interval = interval(100.millis)) {
      commandLineRunner.get(argsParser.parse(Seq("get", "-e", eventKey.key, "-o", "json")).get).await

      val actualEventJson   = removeDynamicKeys(Json.parse(logBuffer.last))
      val expectedEventJson = removeDynamicKeys(addEventIdAndName(eventJson, eventKey))

      actualEventJson shouldBe expectedEventJson
    }
  }

  // DEOPSCSW-432: [Event Cli] Publish command
  test("should able to publish event with interval") {
    val queue             = new mutable.Queue[JsObject]()
    val eventKey          = EventKey("tcs.mobie.blue.filter")
    val path              = getClass.getResource("/publish/observe_event.json").getPath
    val eventJson         = Json.parse(Source.fromResource("publish/observe_event.json").mkString)
    val expectedEventJson = removeDynamicKeys(addEventIdAndName(eventJson, eventKey))

    val subscriber = Await.result(eventService.defaultSubscriber, 5.seconds)
    subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](e ⇒ queue.enqueue(eventToSanitizedJson(e)))).run()
    Thread.sleep(500)

    // publish same event every 300 millis for 2 seconds and starts with 0th sec, which results into publishing 7 events
    commandLineRunner
      .publish(argsParser.parse(Seq("publish", "-e", s"${eventKey.key}", "--data", path, "-i", "300", "-p", "2")).get)
      .await

    // invalid event + 7 events published in previous step
    eventually(queue.size shouldBe 8)
    queue should contain allElementsOf Seq(eventToSanitizedJson(Event.invalidEvent(eventKey))) ++ (1 to 5).map(
      _ ⇒ expectedEventJson
    )
  }

  // publish command generates new id and event time while publishing, hence assertions exclude these keys from json
  private def removeDynamicKeys(json: JsValue) = JsObject(json.as[JsObject].value -- Seq("eventId", "eventTime"))

  // publish command with -e argument updates existing prefix and event name from provided json if already present
  // else adds new entry
  private def addEventIdAndName(json: JsValue, eventKey: EventKey) = json.as[JsObject] ++ Json.obj(
    ("source", eventKey.source.prefix),
    ("eventName", eventKey.eventName.name)
  )

  private def eventToSanitizedJson(event: Event) = removeDynamicKeys(JsonSupport.writeEvent(event))
  private def stringToEvent(eventString: String) = JsonSupport.readEvent[Event](Json.parse(eventString))

  test("should able to subscribe to event key") {
    import cliWiring._
    implicit val ec    = actorRuntime.ec
    val eventGenerator = new EventGenerator(EventName(s"system_1"))
    import eventGenerator._
    val eventKey: EventKey = eventsGroup.head.eventKey
    val publisher          = eventService.defaultPublisher.await

    publisher.publish(eventGenerator.generate, 400.millis)

    //to publish first event
    Thread.sleep(200)

    commandLineRunner.subscribe(argsParser.parse(Seq("subscribe", "--events", eventKey.key)).get)
    //wait for next 2 events
    Thread.sleep(900)
    val expectedEventGenerator = new EventGenerator(EventName(s"system_1"))
    val expectedEvents         = (1 to 3).map(_ => expectedEventGenerator.generate).toList

    logBuffer.size shouldBe expectedEvents.size
    logBuffer.map(stringToEvent).toList shouldBe expectedEvents
  }
}
