package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceInfo}

case class RegisterResult(registry: JmDNS, info: ServiceInfo, componentId: ComponentId) {
  def unregister(): Unit = registry.unregisterService(info)
}
