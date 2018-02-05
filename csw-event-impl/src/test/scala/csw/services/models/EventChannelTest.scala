package csw.services.models

import csw.messages.ccs.events.{EventName, ObserveEvent, SystemEvent}
import csw.messages.params.models.Prefix
import org.scalatest.{FunSuite, Matchers}

class EventChannelTest extends FunSuite with Matchers {

  test("should be able to create eventkey from SystemEvent") {

    val prefix      = "test.prefix"
    val eventName   = "system"
    val systemEvent = SystemEvent(Prefix(prefix), EventName(eventName))

    systemEvent.eventKey shouldBe prefix + "." + eventName

  }

  test("should be able to create eventkey from ObserveEvent") {

    val prefix       = "test.prefix"
    val eventName    = "observe"
    val observeEvent = ObserveEvent(Prefix(prefix), EventName(eventName))

    observeEvent shouldBe prefix + "." + eventName

  }

}
