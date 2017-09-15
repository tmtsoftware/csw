package csw.common.framework.internal.supervisor

import akka.typed.ActorRef
import csw.common.framework.internal.extensions.RichSystemExtension.RichSystem
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.models.{Component, ComponentInfo, ContainerIdleMessage, SupervisorInfo}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.ComponentLogger

import scala.async.Async._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

class SupervisorInfoFactory(containerName: String) extends ComponentLogger.Simple {

  override protected val componentName: String = containerName

  def make(
      containerRef: ActorRef[ContainerIdleMessage],
      componentInfo: ComponentInfo,
      locationService: LocationService
  ): Future[Option[SupervisorInfo]] = {
    val system                                = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val richSystem                            = new RichSystem(system)
    val registrationFactory                   = new RegistrationFactory
    val pubSubBehaviorFactory                 = new PubSubBehaviorFactory

    async {
      val supervisorBehavior = {
        SupervisorBehaviorFactory.make(
          Some(containerRef),
          componentInfo,
          locationService,
          registrationFactory,
          pubSubBehaviorFactory
        )
      }
      val actorRefF = richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
      Some(SupervisorInfo(system, Component(await(actorRefF), componentInfo)))
    } recover {
      case NonFatal(exception) ⇒
        log.error(s"Exception :[${exception.getMessage}] occurred while spawning supervisor: [${componentInfo.name}]",
                  ex = exception)
        system.terminate()
        None
    }
  }
}
