package csw.services.event.internal.perf

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.stage.GraphStage
import akka.stream.{ActorMaterializer, FlowShape}
import csw.messages.events.{Event, EventKey}
import csw.services.event.helpers.Monitor
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.makeEvent
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, SubscriptionModes}
import org.scalatest.Matchers

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class EventServicePerfFramework(publisher: EventPublisher, subscriber: EventSubscriber)(
    implicit val actorSystem: ActorSystem
) extends Matchers {

  private implicit val mat: ActorMaterializer = ActorMaterializer()

  val eventKey: EventKey = makeEvent(0).eventKey
  var counter            = 0
  //  private val eventStream = Source.repeat(()).map(_ => eventGenerator())

  private def eventGenerator() = {
    counter += 1
    makeEvent(counter)
  }

  def monitorPerf(): Unit = {
    val tickDuration = 20.millis
    val doneF = subscriber
    // uncomment below line and EventSubscriber.subscribeWithSinkActorRef method to see the effects of subscribing with Sink.actorRef
    //.subscribeWithSinkActorRef(Set(eventKey), 20.millis)
      .subscribe(Set(eventKey), tickDuration, SubscriptionModes.RateAdapterMode)
      .via(new Monitor(tickDuration, reportingDuration = 2.seconds).resetting)
      .runWith(Sink.ignore)

    Thread.sleep(1000)

    //    eventStream.mapAsync(1)(publisher.publish).runWith(Sink.ignore)
    //    publisher.publish(eventStream)
    publisher.publish(eventGenerator, 5.millis)

    doneF.await
  }

  def comparePerf(createStage: FiniteDuration => GraphStage[FlowShape[Event, Event]]): Unit = {
    val publisherTick  = 5.millis
    val subscriberTick = 20.millis

    val doneF = subscriber
      .subscribe(Set(eventKey))
      .via(createStage(subscriberTick))
//      .map(x => { println(x.eventId); x })
      .via(new Monitor(subscriberTick, reportingDuration = 2.seconds).resetting)
      .runWith(Sink.ignore)

    Thread.sleep(1000)

    publisher.publish(eventGenerator, publisherTick)

    doneF.await
  }
}
