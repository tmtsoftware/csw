package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.PubSub.{Publish, PublisherMessage}
import csw.messages._
import csw.messages.ccs.ValidationIssue.OtherIssue
import csw.messages.ccs.{Validation, Validations}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.generics.GChoiceKey
import csw.messages.params.generics.KeyType.ChoiceKey
import csw.messages.params.models.{Choice, Choices, Prefix}
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Future

object SampleComponentState {
  val restartChoice         = Choice("Restart")
  val onlineChoice          = Choice("Online")
  val domainChoice          = Choice("Domain")
  val shutdownChoice        = Choice("Shutdown")
  val setupConfigChoice     = Choice("SetupConfig")
  val observeConfigChoice   = Choice("SetupConfig")
  val submitCommandChoice   = Choice("SubmitCommand")
  val oneWayCommandChoice   = Choice("OneWayCommand")
  val initChoice            = Choice("Initialize")
  val offlineChoice         = Choice("Offline")
  val locationUpdatedChoice = Choice("LocationUpdated")
  val locationRemovedChoice = Choice("LocationRemoved")
  val prefix: Prefix        = Prefix("wfos.prog.cloudcover")
  val successPrefix: Prefix = Prefix("wfos.prog.cloudcover.success")
  val failedPrefix: Prefix  = Prefix("wfos.prog.cloudcover.failure")

  val choices: Choices =
    Choices.fromChoices(
      restartChoice,
      onlineChoice,
      domainChoice,
      shutdownChoice,
      setupConfigChoice,
      observeConfigChoice,
      submitCommandChoice,
      oneWayCommandChoice,
      initChoice,
      offlineChoice,
      locationUpdatedChoice,
      locationRemovedChoice
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

  override def onGoOffline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(domainChoice))))
  }

  override def onSetup(commandMessage: CommandMessage): Validation = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(setupConfigChoice))))
    validateCommand(commandMessage)
  }

  override def onObserve(commandMessage: CommandMessage): Validation = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(observeConfigChoice))))
    validateCommand(commandMessage)
  }

  private def validateCommand(commandMsg: CommandMessage): Validation = {
    commandMsg match {
      case Submit(command, replyTo) =>
        pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(submitCommandChoice))))
      case Oneway(command, replyTo) =>
        pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(oneWayCommandChoice))))
    }

    if (commandMsg.command.prefix.prefix.contains("success")) {
      Validations.Valid
    } else {
      Validations.Invalid(OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onShutdown(): Future[Unit] = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(100)
    Future.unit
  }

  override protected def maybeComponentName() = Some(componentInfo.name)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location) =>
      pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(locationUpdatedChoice))))
    case LocationRemoved(connection) =>
      pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(locationRemovedChoice))))
  }
}
