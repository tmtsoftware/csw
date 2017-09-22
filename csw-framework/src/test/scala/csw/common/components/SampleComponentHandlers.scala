package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.ccs.{Validation, Validations}
import csw.framework.models.CommandMessage.{Oneway, Submit}
import csw.framework.models.PubSub.{Publish, PublisherMessage}
import csw.framework.models.{CommandMessage, ComponentInfo, ComponentMessage}
import csw.framework.scaladsl.ComponentHandlers
import csw.param.generics.GChoiceKey
import csw.param.generics.KeyType.ChoiceKey
import csw.param.models.{Choice, Choices, Prefix}
import csw.param.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Future

object SampleComponentState {
  val restartChoice       = Choice("Restart")
  val runChoice           = Choice("Run")
  val onlineChoice        = Choice("Online")
  val domainChoice        = Choice("Domain")
  val shutdownChoice      = Choice("Shutdown")
  val submitCommandChoice = Choice("SubmitCommand")
  val oneWayCommandChoice = Choice("OneWayCommand")
  val initChoice          = Choice("Initialize")
  val offlineChoice       = Choice("Offline")
  val prefix: Prefix      = Prefix("wfos.prog.cloudcover")

  val choices: Choices =
    Choices.fromChoices(
      restartChoice,
      runChoice,
      onlineChoice,
      domainChoice,
      shutdownChoice,
      submitCommandChoice,
      oneWayCommandChoice,
      initChoice,
      offlineChoice
    )
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)
}

class SampleComponentHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[ComponentDomainMessage](ctx, componentInfo, pubSubRef, locationService)
    with ComponentLogger.Simple {

  import SampleComponentState._

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    Future.unit
  }

  override def onRun(): Future[Unit] = {
    Thread.sleep(100)
    Future.successful(pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
  }

  override def onGoOffline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(domainChoice))))
  }

  override def onControlCommand(commandMsg: CommandMessage): Validation = {
    commandMsg match {
      case Submit(command, replyTo) =>
        pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(submitCommandChoice))))
      case Oneway(command, replyTo) =>
        pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(oneWayCommandChoice))))
    }

    Validations.Valid
  }

  override def onShutdown(): Future[Unit] = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(100)
    Future.unit
  }

  override protected def maybeComponentName() = Some(componentInfo.name)
}
