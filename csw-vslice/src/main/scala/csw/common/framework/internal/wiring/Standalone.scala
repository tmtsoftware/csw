package csw.common.framework.internal.wiring

import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.common.framework.models.SupervisorExternalMessage

object Standalone {

  def spawn(config: com.typesafe.config.Config, wiring: FrameworkWiring): ActorRef[SupervisorExternalMessage] = {
    import wiring._
    val componentInfo      = ComponentInfoParser.parseStandalone(config)
    val supervisorBehavior = SupervisorBehaviorFactory.make(None, componentInfo, locationService, registrationFactory)
    actorSystem.spawn(supervisorBehavior, componentInfo.name)
  }
}
