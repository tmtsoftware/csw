package csw.framework.internal.supervisor

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import csw.framework.CurrentStatePublisher
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.framework.models.CswServices
import csw.messages.ContainerIdleMessage
import csw.messages.framework.{Component, ComponentInfo, SupervisorInfo}
import csw.messages.params.states.CurrentState
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.event.EventServiceFactory
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.async.Async._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

/**
 * The factory for creating supervisor actors of a component specified by [[csw.messages.framework.ComponentInfo]]
 */
private[framework] class SupervisorInfoFactory(containerName: String) {
  private val log: Logger                     = new LoggerFactory(containerName).getLogger
  private val PubSubComponentActor            = "pub-sub-component"
  private val CommandResponseManagerActorName = "command-response-manager"

  def make(
      containerRef: ActorRef[ContainerIdleMessage],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      eventServiceFactory: EventServiceFactory,
      alarmServiceFactory: AlarmServiceFactory,
      registrationFactory: RegistrationFactory
  ): Future[Option[SupervisorInfo]] = {
    implicit val system: ActorSystem          = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val richSystem                            = new CswFrameworkSystem(system)

    async {
      val commandResponseManagerFactory = new CommandResponseManagerFactory
      val eventService                  = eventServiceFactory.make(locationService)
      val alarmService                  = alarmServiceFactory.makeClientApi(locationService)
      val loggerFactory                 = new LoggerFactory(componentInfo.name)

      val pubSubComponentActor = await(
        richSystem.spawnTyped(new PubSubBehaviorFactory().make[CurrentState](PubSubComponentActor, loggerFactory),
                              PubSubComponentActor)
      )
      val currentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

      val commandResponseManagerActor =
        await(richSystem.spawnTyped(commandResponseManagerFactory.makeBehavior(loggerFactory), CommandResponseManagerActorName))
      val commandResponseManager = commandResponseManagerFactory.make(commandResponseManagerActor)

      val cswServices = new CswServices(
        locationService,
        eventService,
        alarmService,
        loggerFactory,
        currentStatePublisher,
        commandResponseManager
      )

      val supervisorBehavior = SupervisorBehaviorFactory.make(
        Some(containerRef),
        componentInfo,
        registrationFactory,
        cswServices
      )

      val actorRefF = richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
      Some(SupervisorInfo(system, Component(await(actorRefF), componentInfo)))
    } recoverWith {
      case NonFatal(exception) ⇒
        async {
          log.error(s"Exception :[${exception.getMessage}] occurred while spawning supervisor: [${componentInfo.name}]",
                    ex = exception)
          await(system.terminate())
          None
        }
    }
  }
}
