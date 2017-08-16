package csw.common.components.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.{Validation, Validations}
import csw.common.framework.models.PubSub.{Publish, PublisherMsg}
import csw.common.framework.models.{CommandMsg, ComponentInfo, ComponentMsg}
import csw.common.framework.scaladsl.ComponentHandlers
import csw.param.generics.GChoiceKey
import csw.param.generics.KeyType.ChoiceKey
import csw.param.models.{Choice, Choices, Prefix}
import csw.param.states.CurrentState

import scala.concurrent.Future

object SampleHcdHandlers {
  val restartChoice  = Choice("Restart")
  val runChoice      = Choice("Run")
  val onlineChoice   = Choice("Online")
  val domainChoice   = Choice("Domain")
  val shutdownChoice = Choice("Shutdown")
  val commandChoice  = Choice("Command")
  val initChoice     = Choice("Initialize")
  val offlineChoice  = Choice("Offline")
  val prefix: Prefix = Prefix("wfos.prog.cloudcover")

  val choices: Choices =
    Choices.fromChoices(restartChoice,
                        runChoice,
                        onlineChoice,
                        domainChoice,
                        shutdownChoice,
                        commandChoice,
                        initChoice,
                        offlineChoice)
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)
}

class SampleHcdHandlers(ctx: ActorContext[ComponentMsg],
                        componentInfo: ComponentInfo,
                        pubSubRef: ActorRef[PublisherMsg[CurrentState]])
    extends ComponentHandlers[HcdDomainMsg](ctx, componentInfo, pubSubRef) {
  import SampleHcdHandlers._

  override def onRestart(): Unit  = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(restartChoice))))
  override def onRun(): Unit      = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(runChoice))))
  override def onGoOnline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
  override def onDomainMsg(msg: HcdDomainMsg): Unit =
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(domainChoice))))
  override def onShutdown(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
  override def onControlCommand(commandMsg: CommandMsg): Validation = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(commandChoice))))
    Validations.Valid
  }
  override def initialize(): Future[Unit] = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    Future.unit
  }
  override def onGoOffline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
}
