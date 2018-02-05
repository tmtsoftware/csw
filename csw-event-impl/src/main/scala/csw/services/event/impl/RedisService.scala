package csw.services.event.impl

import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.RedisClient

class RedisService(redisClient: RedisClient) extends EventServiceDriver {

  override def publishToChannel(data: PbEvent, channel: String): Unit = {}

}
