package csw.services.event.internal.publisher

import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

class EventPublisherImplTest extends FunSuite with Matchers with MockitoSugar {
  test("testPublish") {
    val eventServiceDriver = mock[EventServiceDriver]
    val prefix             = Prefix("test.prefix")
    val eventName          = EventName("system")
    val event              = SystemEvent(prefix, eventName)
    when(eventServiceDriver.publish(any[String], any[PbEvent])).thenReturn(Future.unit)
    new EventPublisherImpl(eventServiceDriver).publish(event)

    verify(eventServiceDriver).publish(event.eventKey.toString, Event.typeMapper.toBase(event))
  }

}
