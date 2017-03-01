package csw.services.location.scaladsl

import javax.jmdns.{ServiceEvent, ServiceListener}

class JmDnsEventStream {



  new ServiceListener {
    override def serviceAdded(event: ServiceEvent) = ???

    override def serviceResolved(event: ServiceEvent) = ???

    override def serviceRemoved(event: ServiceEvent) = ???
  }
}
