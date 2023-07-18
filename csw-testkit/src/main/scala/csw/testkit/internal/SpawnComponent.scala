/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit.internal

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.{ComponentMessage, TopLevelActorMessage}
import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.internal.wiring.{CswFrameworkSystem, FrameworkWiring}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
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
      behaviorFactory: (ActorContext[TopLevelActorMessage], CswContext) => ComponentHandlers,
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
        SupervisorBehaviorFactory.make(None, frameworkWiring.registrationFactory, behaviorFactory, cswCtx)

      await(richSystem.spawnTyped(supervisorBehavior, componentInfo.prefix.toString))
    }
  }
}
