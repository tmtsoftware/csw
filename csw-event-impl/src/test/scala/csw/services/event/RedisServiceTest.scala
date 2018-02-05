package csw.services.event

import csw.messages.ccs.events.{EventName, ObserveEvent, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.impl.RedisService
import csw.services.models.EventChannel
import io.lettuce.core.RedisClient
import org.scalatest.{FunSuite, Matchers}

class RedisServiceTest extends FunSuite with Matchers {

  test("should be able to create channel from SystemEvent") {

    val prefix      = "test.prefix"
    val eventName   = "system"
    val systemEvent = SystemEvent(Prefix(prefix), EventName(eventName))

    EventChannel(systemEvent).channel shouldBe prefix + "." + eventName

  }

  test("should be able to create channel from ObserveEvent") {

    val prefix      = "test.prefix"
    val eventName   = "observe"
    val systemEvent = ObserveEvent(Prefix(prefix), EventName(eventName))

    EventChannel(systemEvent).channel shouldBe prefix + "." + eventName

  }

}
