package csw.framework.internal.supervisor

import akka.typed.ActorRef
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.framework.models.ComponentInfo
import csw.messages.messages.{Component, ContainerIdleMessage, SupervisorInfo}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.ComponentLogger

import scala.async.Async._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

class SupervisorInfoFactory(containerName: String) extends ComponentLogger.Simple {

  override protected def maybeComponentName(): Option[String] = Some(containerName)

  def make(
      containerRef: ActorRef[ContainerIdleMessage],
      componentInfo: ComponentInfo,
      locationService: LocationService
  ): Future[Option[SupervisorInfo]] = {
    val system                                = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val richSystem                            = new CswFrameworkSystem(system)
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
      Some(SupervisorInfo(system, Component(await(actorRefF), componentInfo.getSerializableInfo)))
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
