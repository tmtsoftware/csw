package csw.services.event.internal

import java.util.concurrent.LinkedBlockingQueue

import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.RedisPubSubListener

import scala.collection.mutable

class TestRedisPubSubListener extends RedisPubSubListener[String, PbEvent] {

  val channels = new mutable.MutableList[String]
  val messages = new mutable.MutableList[PbEvent]
  val counts   = new mutable.MutableList[Long]

  override def message(channel: String, message: PbEvent): Unit = messages += message

  override def message(pattern: String, channel: String, message: PbEvent): Unit = {}

  override def psubscribed(pattern: String, count: Long): Unit = {}

  override def subscribed(channel: String, count: Long): Unit = channels ++ channel

  override def unsubscribed(channel: String, count: Long): Unit = {}

  override def punsubscribed(pattern: String, count: Long): Unit = {}
}
