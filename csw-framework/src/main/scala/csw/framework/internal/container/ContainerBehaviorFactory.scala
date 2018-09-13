package csw.framework.internal.container

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.command.messages.ContainerActorMessage
import csw.framework.internal.supervisor.SupervisorInfoFactory
import csw.framework.models.ContainerInfo
import csw.services.location.api.scaladsl.LocationService
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.event.EventServiceFactory
import csw.services.location.scaladsl.RegistrationFactory
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.actor.typed.scaladsl.MutableBehavior]] of a container component
 */
private[framework] object ContainerBehaviorFactory {
  def behavior(
      containerInfo: ContainerInfo,
      locationService: LocationService,
      eventServiceFactory: EventServiceFactory,
      alarmServiceFactory: AlarmServiceFactory,
      registrationFactory: RegistrationFactory
  ): Behavior[ContainerActorMessage] = {
    val supervisorFactory = new SupervisorInfoFactory(containerInfo.name)
    val loggerFactory     = new LoggerFactory(containerInfo.name)
    Behaviors.setup(
      ctx ⇒
        new ContainerBehavior(
          ctx,
          containerInfo,
          supervisorFactory,
          registrationFactory,
          locationService,
          eventServiceFactory,
          alarmServiceFactory,
          loggerFactory
      )
    )
  }
}
