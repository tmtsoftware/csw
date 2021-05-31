package csw.testkit.internal

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.internal.wiring.{CswFrameworkSystem, FrameworkWiring}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.location.api.models.{ComponentType, Connection}
import csw.prefix.models.Prefix

import scala.async.Async.{async, await}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}

private[testkit] object SpawnComponent {

  def spawnComponent(
      frameworkWiring: FrameworkWiring,
      prefix: Prefix,
      componentType: ComponentType,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage,
      connections: Set[Connection],
      initializeTimeout: FiniteDuration
  ): Future[ActorRef[ComponentMessage]] = {
    implicit val ec: ExecutionContextExecutor   = frameworkWiring.actorRuntime.ec
    implicit val richSystem: CswFrameworkSystem = new CswFrameworkSystem(frameworkWiring.actorRuntime.actorSystem)
    val componentInfo =
      ComponentInfo(prefix, componentType, "", locationServiceUsage, connections, initializeTimeout)

    async {
      val cswCtx =
        await(
          CswContext.make(
            frameworkWiring.locationService,
            frameworkWiring.eventServiceFactory,
            frameworkWiring.alarmServiceFactory,
            componentInfo
          )
        )
      val supervisorBehavior =
        SupervisorBehaviorFactory.make(None, frameworkWiring.registrationFactory, behaviorFactory, cswCtx, None)

      await(richSystem.spawnTyped(supervisorBehavior, componentInfo.prefix.toString))
    }
  }
}
