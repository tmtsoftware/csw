package csw.services.location.internal.wrappers

import javax.jmdns.{JmDNS, ServiceEvent, ServiceInfo, ServiceListener}

import akka.stream.scaladsl.SourceQueueWithComplete
import csw.services.location.internal.{Constants, StreamExt}
import csw.services.location.internal.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models._
import csw.services.location.scaladsl.ActorRuntime

class JmDnsReal(jmDns: JmDNS, queue: SourceQueueWithComplete[Location], actorRuntime: ActorRuntime) extends JmDnsApi {

  import actorRuntime._

  jmDns.addServiceListener(Constants.DnsType, makeListener(queue))

  def registerService(reg: Registration): Unit = {
    jmDns.registerService(reg.serviceInfo)
  }

  def unregisterService(connection: Connection): Unit = {
    jmDns.unregisterService(ServiceInfo.create(Constants.DnsType, connection.name, 0, ""))
  }

  def unregisterAllServices(): Unit = {
    jmDns.unregisterAllServices()
    Thread.sleep(4000)
  }

  def requestServiceInfo(connection: Connection): Unit = {
    jmDns.requestServiceInfo(Constants.DnsType, connection.name, true)
  }

  def list(typeName: String): List[Location] = {
    jmDns.list(Constants.DnsType).toList.flatMap(_.locations)
  }

  def close(): Unit = jmDns.close()

  private def makeListener(queue: SourceQueueWithComplete[Location]) = new ServiceListener {
    override def serviceAdded(event: ServiceEvent): Unit = {
      Connection.parse(event.getName).map(conn => queue.offer(Unresolved(conn)))
    }

    override def serviceResolved(event: ServiceEvent): Unit = {
      event.getInfo.locations.foreach(location => queue.offer(location))
    }

    override def serviceRemoved(event: ServiceEvent): Unit = {
      Connection.parse(event.getName).map(conn => queue.offer(Removed(conn)))
    }
  }

}

object JmDnsReal extends JmDnsApiFactory {
  def make(actorRuntime: ActorRuntime, queue: SourceQueueWithComplete[Location]): JmDnsApi = {
    val jmDNS = JmDNS.create(actorRuntime.ipaddr)
    new JmDnsReal(jmDNS, queue, actorRuntime)
  }
}
