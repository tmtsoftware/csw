package csw.services.event.internal.wiring

import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.javadsl.{IEventPublisher, IEventSubscriber}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

trait BaseProperties {
  val wiring: Wiring
  val eventPattern: String
  def publisher: EventPublisher
  def subscriber: EventSubscriber
  def jPublisher[T <: EventPublisher]: IEventPublisher
  def jSubscriber[T <: EventSubscriber]: IEventSubscriber

}

object BaseProperties {
  def createInfra(seedPort: Int, serverPort: Int): (ClusterSettings, LocationService) = {
    val clusterSettings: ClusterSettings = ClusterAwareSettings.joinLocal(seedPort)
    val locationService                  = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
    val tcpRegistration                  = RegistrationFactory.tcp(EventServiceConnection.value, serverPort)
    locationService.register(tcpRegistration).await
    (clusterSettings, locationService)
  }
}
