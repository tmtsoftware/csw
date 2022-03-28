/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.container

import akka.actor.typed.{ActorRefResolver, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.messages.ContainerActorMessage
import csw.event.client.EventServiceFactory
import csw.framework.internal.supervisor.SupervisorInfoFactory
import csw.framework.models.ContainerInfo
import csw.framework.scaladsl.RegistrationFactory
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.actor.typed.scaladsl.AbstractBehavior]] of a container component
 */
private[framework] object ContainerBehaviorFactory {
  def behavior(
      containerInfo: ContainerInfo,
      locationService: LocationService,
      eventServiceFactory: EventServiceFactory,
      alarmServiceFactory: AlarmServiceFactory,
      registrationFactory: RegistrationFactory
  ): Behavior[ContainerActorMessage] = {
    val supervisorFactory = new SupervisorInfoFactory(containerInfo.prefix)
    val loggerFactory     = new LoggerFactory(containerInfo.prefix)
    Behaviors.setup(ctx =>
      new ContainerBehavior(
        ctx,
        containerInfo,
        supervisorFactory,
        registrationFactory,
        locationService,
        eventServiceFactory,
        alarmServiceFactory,
        loggerFactory,
        ActorRefResolver(ctx.system)
      )
    )
  }
}
