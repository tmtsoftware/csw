package csw.common.framework.internal.wiring

import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.common.framework.models.SupervisorExternalMessage
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.{LocationServiceFactory, RegistrationFactory}

object Standalone {
  def spawn(config: com.typesafe.config.Config): ActorRef[SupervisorExternalMessage] = {
    val componentInfo       = ComponentInfoParser.parseStandalone(config)
    val system              = ClusterAwareSettings.system
    val locationService     = LocationServiceFactory.make()
    val registrationFactory = new RegistrationFactory
    val supervisorBehavior  = SupervisorBehaviorFactory.behavior(componentInfo, locationService, registrationFactory)
    system.spawn(supervisorBehavior, componentInfo.name)
  }
}
