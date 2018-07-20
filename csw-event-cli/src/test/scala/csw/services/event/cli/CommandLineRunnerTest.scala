package csw.services.event.cli

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.{IntKey, StringKey}
import csw.messages.params.models.Id
import csw.messages.params.models.Units.meter
import csw.services.event.cli.IterableExtensions.RichStringIterable
import csw.services.event.helpers.TestFutureExt.RichFuture
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json._

import scala.collection.{immutable, mutable}
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source

class CommandLineRunnerTest extends FunSuite with Matchers with SeedData with Eventually {

  def events(name: EventName): immutable.Seq[Event] =
    for (i ← 1 to 10) yield event1.copy(eventName = name, eventId = Id(i.toString))

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
  test("should able to inspect events containing multiple parameters including recursive structs") {

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "-e", s"${event1.eventKey},${event2.eventKey}")).get).await
    logBuffer shouldEqualContentsOf "oneline/inspect_multiple_events.txt"
  }

  // DEOPSCSW-431: [Event Cli] Get command
  test("should able to get events in json format") {

    commandLineRunner.get(argsParser.parse(Seq("get", "-e", event1.eventKey.key, "-o", "json")).get).await
    stringToEvent(logBuffer.head) shouldBe event1

    logBuffer.clear()

    commandLineRunner.get(argsParser.parse(Seq("get", "-e", s"${event1.eventKey},${event2.eventKey}", "--out", "json")).get).await
    val events = logBuffer.map(stringToEvent).toSet
    events shouldEqual Set(event1, event2)
  }

  // DEOPSCSW-431: [Event Cli] Get command
  test("should able to get appropriate error message when there is no event published for provided key") {
    commandLineRunner.get(argsParser.parse(Seq("get", "-e", "x.y.invalid_key", "--out", "json")).get).await
    logBuffer.head shouldEqual "[ERROR] No events published for key: [x.y.invalid_key]"
  }

  // DEOPSCSW-431: [Event Cli] Get command
  test("should able to get events in oneline format") {

    commandLineRunner
      .get(argsParser.parse(Seq("get", "--id", "-u", "-t", "-e", s"${event1.eventKey},${event2.eventKey}")).get)
      .await
    logBuffer shouldEqualContentsOf "oneline/get_multiple_events.txt"
  }

  // DEOPSCSW-432: [Event Cli] Publish command
  test("should able to publish event when event key and event json file provided") {
    val path      = getClass.getResource("/publish/observe_event.json").getPath
    val eventJson = Json.parse(Source.fromResource("publish/observe_event.json").mkString)

    val eventKey = EventKey("wfos.blue.filter.wheel")
    commandLineRunner.publish(argsParser.parse(Seq("publish", "-e", eventKey.key, "--data", path)).get).await

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
      .publish(argsParser.parse(Seq("publish", "-e", eventKey.key, "--data", path, "-i", "300", "-p", "2")).get)
      .await

    // invalid event + 7 events published in previous step
    eventually(queue.size shouldBe 8)
    queue should contain allElementsOf Seq(eventToSanitizedJson(Event.invalidEvent(eventKey))) ++ (1 to 5).map(
      _ ⇒ expectedEventJson
    )
  }

  // DEOPSCSW-436: [Event Cli] Specialized Publish command (take params from command line)
  test("should able to publish event when params are provided") {
    val filePath = "publish/observe_event.json"
    val path     = getClass.getResource("/" + filePath).getPath
    val eventKey = EventKey("wfos.test.move")

    val cmdLineParams = "testKey:s=[test]|testKey2:i:meter=[1,2,3]"
    val strParam      = StringKey.make("testKey").set("test")
    val intParam      = IntKey.make("testKey2").set(1, 2, 3).withUnits(meter)

    val expectedEvent     = fileToEvent[ObserveEvent](filePath).madd(strParam, intParam)
    val expectedEventJson = JsonSupport.writeEvent[ObserveEvent](expectedEvent)

    // first publish event with default data from json file
    commandLineRunner.publish(argsParser.parse(Seq("publish", "-e", eventKey.key, "--data", path)).get).await

    Thread.sleep(500)

    // publish with params and verify new event contains existing params as well as newly provided cmd line params
    commandLineRunner.publish(argsParser.parse(Seq("publish", "-e", eventKey.key, "--params", cmdLineParams)).get).await

    eventually(timeout = timeout(5.seconds), interval = interval(100.millis)) {
      commandLineRunner.get(argsParser.parse(Seq("get", "-e", eventKey.key, "-o", "json")).get).await
      removeDynamicKeys(strToJsObject(logBuffer.last)) shouldBe removeDynamicKeys(addEventIdAndName(expectedEventJson, eventKey))
    }

    // verify existing params get updated if provided cmd line params already present
    val updateCmdLineParams = "testKey:s=['test1','test2']|testKey2:i:meter=[4]"
    val updatedStrParam     = StringKey.make("testKey").set("test1", "test2")
    val updatedIntParam     = IntKey.make("testKey2").set(4).withUnits(meter)

    val updatedExpectedEvent     = fileToEvent[ObserveEvent](filePath).madd(updatedStrParam, updatedIntParam)
    val updatedExpectedEventJson = JsonSupport.writeEvent[ObserveEvent](updatedExpectedEvent)

    commandLineRunner
      .publish(argsParser.parse(Seq("publish", "-e", eventKey.key, "--params", updateCmdLineParams)).get)
      .await

    eventually(timeout = timeout(5.seconds), interval = interval(100.millis)) {
      commandLineRunner.get(argsParser.parse(Seq("get", "-e", eventKey.key, "-o", "json")).get).await
      removeDynamicKeys(strToJsObject(logBuffer.last)) shouldBe removeDynamicKeys(
        addEventIdAndName(updatedExpectedEventJson, eventKey)
      )
    }
  }

  // DEOPSCSW-433: [Event Cli] Subscribe command
  test("should be able to subscribe and get json output to event key") {

    implicit val mat: Materializer    = actorRuntime.mat
    implicit val ec: ExecutionContext = actorRuntime.ec

    val eventGenerator = new EventGenerator(EventName("system_1"))
    import eventGenerator._
    val publisher   = eventService.defaultPublisher.await
    val cancellable = publisher.publish(eventGenerator.generate, 400.millis)

    val eventKey: EventKey = eventsGroup.head.eventKey
    val (subscriptionF, _) =
      commandLineRunner.subscribe(argsParser.parse(Seq("subscribe", "-o", "json", "--events", eventKey.key)).get)

    Thread.sleep(1000)

    cancellable.cancel()
    subscriptionF.map(_.unsubscribe())

    logBuffer shouldEqualContentsOf "json/entire_events.txt"
  }

  // DEOPSCSW-433: [Event Cli] Subscribe command
  test("should be able to subscribe to event key and get oneline output") {
    import cliWiring._

    implicit val mat: Materializer    = actorRuntime.mat
    implicit val ec: ExecutionContext = actorRuntime.ec

    val eventGenerator = new EventGenerator(EventName("system_2"))
    import eventGenerator._

    val eventKey: EventKey = eventsGroup.head.eventKey
    val publisher          = eventService.defaultPublisher.await

    val (subscriptionF, _) =
      commandLineRunner.subscribe(argsParser.parse(Seq("subscribe", "--events", eventKey.key, "--id")).get)

    Thread.sleep(500)
    val cancellable = publisher.publish(eventGenerator.generate, 400.millis)

    Thread.sleep(1000)
    cancellable.cancel()
    subscriptionF.map(_.unsubscribe())

    logBuffer shouldEqualContentsOf "oneline/entire_events.txt"
  }

  // publish command generates new id and event time while publishing, hence assertions exclude these keys from json
  private def removeDynamicKeys(json: JsValue) = JsObject(json.as[JsObject].value -- Seq("eventId", "eventTime"))

  // publish command with -e argument updates existing prefix and event name from provided json if already present
  // else adds new entry
  private def addEventIdAndName(json: JsValue, eventKey: EventKey) = json.as[JsObject] ++ Json.obj(
    ("source", eventKey.source.prefix),
    ("eventName", eventKey.eventName.name)
  )

  private def eventToSanitizedJson(event: Event)        = removeDynamicKeys(JsonSupport.writeEvent(event))
  private def stringToEvent(eventString: String)        = JsonSupport.readEvent[Event](Json.parse(eventString))
  private def fileToEvent[T <: Event](filePath: String) = JsonSupport.readEvent[T](fileToEventJson(filePath))
  private def fileToEventJson(filePath: String)         = Json.parse(Source.fromResource(filePath).mkString)
  private def strToJsObject(js: String)                 = Json.parse(js).as[JsObject]
}
