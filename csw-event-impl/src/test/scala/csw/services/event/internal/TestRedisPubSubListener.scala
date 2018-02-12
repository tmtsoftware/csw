package csw.services.event.internal

import java.util.concurrent.LinkedBlockingQueue

import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.RedisPubSubListener

class TestRedisPubSubListener extends RedisPubSubListener[String, PbEvent] {

  val channels = new LinkedBlockingQueue[String]
  val messages = new LinkedBlockingQueue[PbEvent]
  val counts   = new LinkedBlockingQueue[Long]

  override def message(channel: String, message: PbEvent): Unit = {
//    println(s"*******$message")
    messages.add(message)
  }

  override def message(pattern: String, channel: String, message: PbEvent): Unit = {
//    println(s"*******$message")
    messages.add(message)
  }

  override def psubscribed(pattern: String, count: Long): Unit = {}

  override def subscribed(channel: String, count: Long): Unit = {
//    println(s"cahnnel==*******$channel")
    channels.add(channel)
  }

  override def unsubscribed(channel: String, count: Long): Unit = ???

  override def punsubscribed(pattern: String, count: Long): Unit = ???
}
