package csw.services.location.internal.wrappers

import akka.stream.scaladsl.SourceQueueWithComplete
import csw.services.location.models._
import csw.services.location.scaladsl.ActorRuntime

trait JmDnsApi {

  def registerService(reg: Registration): Unit

  def unregisterService(connection: Connection): Unit

  def unregisterAllServices(): Unit

  def requestServiceInfo(connection: Connection): Unit

  def list(typeName: String): List[Location]

  def close(): Unit
}

trait JmDnsApiFactory {
  def make(actorRuntime: ActorRuntime, queue: SourceQueueWithComplete[Location]): JmDnsApi
}
