package csw.framework.internal.wiring

import akka.typed.ActorRef
import csw.framework.internal.configparser.ComponentInfoParser
import csw.framework.internal.extensions.RichSystemExtension.RichSystem
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.models.SupervisorExternalMessage

import scala.concurrent.Future

object Standalone {

  def spawn(config: com.typesafe.config.Config,
            wiring: FrameworkWiring): Future[ActorRef[SupervisorExternalMessage]] = {
    import wiring._
    val componentInfo = ComponentInfoParser.parseStandalone(config)
    val supervisorBehavior = SupervisorBehaviorFactory.make(
      None,
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory
    )
    val richSystem = new RichSystem(actorSystem)
    richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
  }
}
