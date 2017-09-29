package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.models._
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.generics.GChoiceKey
import csw.messages.generics.KeyType.ChoiceKey
import csw.messages.messages.CommandMessage.{Oneway, Submit}
import csw.messages.messages.PubSub.{Publish, PublisherMessage}
import csw.messages.messages._
import csw.messages.models.ccs.ValidationIssue.OtherIssue
import csw.messages.models.ccs.{Validation, Validations}
import csw.messages.models.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.models.params.{Choice, Choices, Prefix}
import csw.messages.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Future

object SampleComponentState {
  val restartChoice         = Choice("Restart")
  val runChoice             = Choice("Run")
  val onlineChoice          = Choice("Online")
  val domainChoice          = Choice("Domain")
  val shutdownChoice        = Choice("Shutdown")
  val submitCommandChoice   = Choice("SubmitCommand")
  val invalidCommandChoice  = Choice("InvalidCommandChoice")
  val validCommandChoice    = Choice("ValidCommandChoice")
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
      runChoice,
      onlineChoice,
      domainChoice,
      shutdownChoice,
      submitCommandChoice,
      invalidCommandChoice,
      validCommandChoice,
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

  override def onCommandValidationNotification(validationResponse: CommandValidationResponse): Unit = {
    validationResponse match {
      case Invalid(issue) ⇒ pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(invalidCommandChoice))))
      case Accepted       ⇒ pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(validCommandChoice))))
    }
  }

  override def onCommandExecutionNotification(executionResponse: CommandExecutionResponse): Unit = {}

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location) =>
      pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(locationUpdatedChoice))))
    case LocationRemoved(connection) =>
      pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(locationRemovedChoice))))
  }
}
