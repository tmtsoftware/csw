package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, PostStop, Signal, SupervisorStrategy, Terminated}
import csw.common.framework.exceptions.FailureRestart
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.internal.supervisor.SupervisorMode.Idle
import csw.common.framework.models.FromComponentLifecycleMessage.Running
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorMode,
  LifecycleStateSubscription
}
import csw.common.framework.models.SupervisorIdleMessage._
import csw.common.framework.models.SupervisorRestartMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.{Failure, Success}

object SupervisorBehavior {
  val PubSubComponentActor              = "pub-sub-component"
  val ComponentActor                    = "component"
  val PubSubLifecycleActor              = "pub-sub-lifecycle"
  val InitializeTimerKey                = "initialize-timer"
  val initializeTimeout: FiniteDuration = 5.seconds
}

class SupervisorBehavior(
    ctx: ActorContext[SupervisorMessage],
    timerScheduler: TimerScheduler[SupervisorMessage],
    maybeContainerRef: Option[ActorRef[ContainerIdleMessage]],
    componentInfo: ComponentInfo,
    componentBehaviorFactory: ComponentBehaviorFactory[_],
    pubSubBehaviorFactory: PubSubBehaviorFactory,
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends MutableBehavior[SupervisorMessage] {

  import SupervisorBehavior._

  implicit val ec: ExecutionContext = ctx.executionContext

  val name: String                                       = componentInfo.name
  val componentId                                        = ComponentId(name, componentInfo.componentType)
  val akkaRegistration: AkkaRegistration                 = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var haltingFlag                                        = false
  var mode: SupervisorMode                               = Idle
  var runningComponent: Option[ActorRef[RunningMessage]] = None
  var registrationOpt: Option[RegistrationResult]        = None
  var component: ActorRef[Nothing]                       = _

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] = pubSubBehaviorFactory.make(ctx, PubSubLifecycleActor)
  val pubSubComponent: ActorRef[PubSub[CurrentState]]          = pubSubBehaviorFactory.make(ctx, PubSubComponentActor)

  registerWithLocationService()

  override def onMessage(msg: SupervisorMessage): Behavior[SupervisorMessage] = {
    (mode, msg) match {
      case (_, msg: SupervisorCommonMessage)                                                       ⇒ onCommon(msg)
      case (SupervisorMode.Idle, msg: SupervisorIdleMessage)                                       ⇒ onIdle(msg)
      case (SupervisorMode.Running | SupervisorMode.RunningOffline, msg: SupervisorRunningMessage) ⇒ onRunning(msg)
      case (SupervisorMode.Restart, msg: SupervisorRestartMessage)                                 ⇒ onRestarting(msg)
      case (_, message) =>
        println(s"Supervisor in $mode received an unexpected message: $message")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case Terminated(componentRef) ⇒
      println(s"log.error(mode is $mode and actor is $componentRef)") //FIXME use log statement
      mode match {
        case SupervisorMode.Restart ⇒
          mode = SupervisorMode.Idle
          registerWithLocationService()
        case SupervisorMode.Shutdown ⇒
          ctx.system.terminate()
        case _ ⇒
      }
      this
    case PostStop ⇒
      registrationOpt.foreach(registrationResult ⇒ registrationResult.unregister())
      this
  }

  def onCommon(msg: SupervisorCommonMessage): Unit = msg match {
    case LifecycleStateSubscription(subscriberMessage) ⇒ pubSubLifecycle ! subscriberMessage
    case ComponentStateSubscription(subscriberMessage) ⇒ pubSubComponent ! subscriberMessage
    case GetSupervisorMode(replyTo)                    ⇒ replyTo ! mode
    case Shutdown                                      ⇒ mode = SupervisorMode.Shutdown; ctx.stop(component);
    case Restart                                       ⇒ onRestart()
  }

  def onIdle(msg: SupervisorIdleMessage): Unit = msg match {
    case RegistrationComplete(registrationResult) ⇒
      registrationOpt = Some(registrationResult)
      spawnAndWatchComponent()
    case RegistrationFailed(throwable) ⇒
      println(s"log.error($throwable)") //FIXME use log statement
    case Running(componentRef) ⇒
      mode = SupervisorMode.Running
      timerScheduler.cancel(InitializeTimerKey)
      runningComponent = Some(componentRef)
      maybeContainerRef foreach (_ ! SupervisorModeChanged(ctx.self, mode))
      pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorMode.Running))
    case InitializeTimeout ⇒
      println("TLA initialization timed out") //FIXME use log statement
    case RunTimeout ⇒
      println("TLA onRun timed out") //FIXME use log statement
  }

  private def onRunning(supervisorRunningMessage: SupervisorRunningMessage): Unit = {
    supervisorRunningMessage match {
      case runningMessage: RunningMessage ⇒
        runningMessage match {
          case Lifecycle(message) ⇒ onLifeCycle(message)
          case _                  ⇒
        }
        runningComponent.get ! runningMessage
    }
  }

  private def onRestart(): Unit = {
    mode = SupervisorMode.Restart
    registrationOpt match {
      case Some(registrationResult) ⇒
        unRegisterFromLocationService(registrationResult)
      case None ⇒
        println("log.warn(No valid RegistrationResult found to unregister.)") //FIXME use log statement
        respawnComponent()
    }
  }

  private def onRestarting(msg: SupervisorRestartMessage): Unit = msg match {
    case UnRegistrationComplete ⇒
      respawnComponent()
    case UnRegistrationFailed(throwable) ⇒
      println(s"log.error($throwable)") //FIXME use log statement
      respawnComponent()
  }

  private def respawnComponent(): Unit = {
//    registrationOpt = None
    ctx.child(ComponentActor) match {
      case Some(componentRef) ⇒
        ctx.stop(component)
      case None ⇒
        mode = SupervisorMode.Idle
        registerWithLocationService()
    }
  }

  private def onLifeCycle(message: ToComponentLifecycleMessage): Unit = {
    message match {
      case GoOffline ⇒ if (mode == SupervisorMode.Running) mode = SupervisorMode.RunningOffline
      case GoOnline  ⇒ if (mode == SupervisorMode.RunningOffline) mode = SupervisorMode.Running
    }
  }

  private def registerWithLocationService(): Unit = {
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def unRegisterFromLocationService(registrationResult: RegistrationResult): Unit = {
    registrationResult.unregister().onComplete {
      case Success(_)         ⇒ ctx.self ! UnRegistrationComplete
      case Failure(throwable) ⇒ ctx.self ! UnRegistrationFailed(throwable)
    }
  }

  private def spawnAndWatchComponent(): Unit = {
    component = ctx.spawn[Nothing](
      Actor
        .supervise[Nothing](componentBehaviorFactory.make(componentInfo, ctx.self, pubSubComponent))
        .onFailure[FailureRestart](SupervisorStrategy.restart.withLoggingEnabled(true)), //FIXME to disable logs in test
      ComponentActor
    )
    ctx.watch(component)
    timerScheduler.startSingleTimer(InitializeTimerKey, InitializeTimeout, initializeTimeout)
  }
}
