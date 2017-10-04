package csw.framework.internal.wiring

import akka.typed.ActorRef
import csw.framework.internal.configparser.ComponentInfoParser
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.messages.SupervisorExternalMessage
import csw.services.logging.internal.LogControlMessages

import scala.concurrent.Future

object Standalone {

  def spawn(
      config: com.typesafe.config.Config,
      wiring: FrameworkWiring,
      adminActorRef: ActorRef[LogControlMessages]
  ): Future[ActorRef[SupervisorExternalMessage]] = {
    import wiring._
    val componentInfo = ComponentInfoParser.parseStandalone(config)
    val supervisorBehavior = SupervisorBehaviorFactory.make(
      None,
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory,
      adminActorRef
    )
    val richSystem = new CswFrameworkSystem(actorSystem)
    richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
  }
}
