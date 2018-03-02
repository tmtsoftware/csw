package csw.framework.internal.container

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.framework.internal.supervisor.SupervisorInfoFactory
import csw.framework.models.ContainerInfo
import csw.messages.ContainerMessage
import csw.services.location.commons.RegistrationFactory
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.typed.scaladsl.Actor.MutableBehavior]] of a container component
 */
private[framework] object ContainerBehaviorFactory {
  def behavior(
      containerInfo: ContainerInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory
  ): Behavior[ContainerMessage] = {
    val supervisorFactory = new SupervisorInfoFactory(containerInfo.name)
    val loggerFactory     = new LoggerFactory(containerInfo.name)
    Actor.mutable(
      ctx ⇒ new ContainerBehavior(ctx, containerInfo, supervisorFactory, registrationFactory, locationService, loggerFactory)
    )
  }
}
