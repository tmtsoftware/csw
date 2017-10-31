package csw.framework.internal.wiring

import akka.typed.ActorRef
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.messages.SupervisorExternalMessage

import scala.concurrent.Future

object Standalone {

  def spawn(
      config: com.typesafe.config.Config,
      wiring: FrameworkWiring
  ): Future[ActorRef[SupervisorExternalMessage]] = {
    import wiring._
    val componentInfo = ConfigParser.parseStandalone(config)
    val supervisorBehavior = SupervisorBehaviorFactory.make(
      None,
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory
    )
    val richSystem = new CswFrameworkSystem(actorSystem)
    richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
  }
}
