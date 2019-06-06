package csw.common.components.command

import csw.params.commands.CommandName
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.generics.{GChoiceKey, KeyType}
import csw.params.core.models.{Choice, Choices, Prefix}
import csw.params.core.states.StateName

object CommandComponentState {
  val encoder = KeyType.IntKey.make("encoder")

  val sourcePrefix  = Prefix("wfos.blue.seq")
  val filterPrefix  = Prefix("wfos.blue.filter")
  val invalidPrefix = Prefix("wfos.blue.filter.invalid")

  val moveCmd                   = CommandName("move")
  val initCmd                   = CommandName("init")
  val acceptedCmd               = CommandName("move.accepted")
  val longRunningCmd            = CommandName("move.accept.result")
  val longRunningCmdToHcd       = CommandName("move.accept.tohcd")
  val onewayCmd                 = CommandName("move.oneway.accept")
  val matcherCmd                = CommandName("move.accept.matcher.success.result")
  val matcherFailedCmd          = CommandName("move.accept.matcher.failed.result")
  val matcherTimeoutCmd         = CommandName("move.accept.matcher.success.timeout")
  val assemCurrentStateCmd      = CommandName("assem.send.current.state")
  val hcdCurrentStateCmd        = CommandName("hcd.send.current.state")
  val crmParentCommandCmd       = CommandName("hcd.parent.crm")
  val crmAddOrUpdateCmd         = CommandName("hcd.update.crm")
  val immediateCmd              = CommandName("move.immediate")
  val immediateResCmd           = CommandName("move.immediate.result")
  val invalidCmd                = CommandName("move.failure")
  val cancelCmd                 = CommandName("move.cancel")
  val failureAfterValidationCmd = CommandName("move.accept.failure")

  val longRunning   = CommandName("move.longCmd")
  val shortRunning  = CommandName("move.shortCmd")
  val mediumRunning = CommandName("move.mediumCmd")

  val longRunningCmdCompleted  = Choice("Long Running Cmd Completed")
  val longRunningCurrentStatus = Choice("Long Running Cmd Completed")
  val shortCmdCompleted        = Choice("Short Running Sub Cmd Completed")
  val mediumCmdCompleted       = Choice("Medium Running Sub Cmd Completed")
  val longCmdCompleted         = Choice("Long Running Sub Cmd Completed")

  val commandChoices: Choices = Choices.fromChoices(
    shortCmdCompleted,
    mediumCmdCompleted,
    longCmdCompleted,
    longRunningCmdCompleted,
    longRunningCurrentStatus
  )
  val commandChoiceKey: GChoiceKey = ChoiceKey.make("choiceKey", commandChoices)

  val restartChoice             = Choice("Restart")
  val onlineChoice              = Choice("Online")
  val domainChoice              = Choice("Domain")
  val shutdownChoice            = Choice("Shutdown")
  val setupConfigChoice         = Choice("SetupConfig")
  val observeConfigChoice       = Choice("ObserveConfig")
  val commandValidationChoice   = Choice("CommandValidation")
  val submitCommandChoice       = Choice("SubmitCommand")
  val oneWayCommandChoice       = Choice("OneWayCommand")
  val initChoice                = Choice("Initialize")
  val offlineChoice             = Choice("Offline")
  val akkaLocationUpdatedChoice = Choice("LocationUpdated")
  val akkaLocationRemovedChoice = Choice("LocationRemoved")
  val httpLocationUpdatedChoice = Choice("HttpLocationUpdated")
  val httpLocationRemovedChoice = Choice("HttpLocationRemoved")
  val tcpLocationUpdatedChoice  = Choice("TcpLocationUpdated")
  val tcpLocationRemovedChoice  = Choice("TcpLocationRemoved")
  val eventReceivedChoice       = Choice("EventReceived")
  val choices: Choices =
    Choices.fromChoices(
      restartChoice,
      onlineChoice,
      domainChoice,
      shutdownChoice,
      setupConfigChoice,
      observeConfigChoice,
      commandValidationChoice,
      submitCommandChoice,
      oneWayCommandChoice,
      initChoice,
      offlineChoice,
      akkaLocationUpdatedChoice,
      akkaLocationRemovedChoice,
      httpLocationUpdatedChoice,
      httpLocationRemovedChoice,
      tcpLocationUpdatedChoice,
      tcpLocationRemovedChoice,
      eventReceivedChoice
    )
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)

  // States
  val timeServiceSchedulerState = StateName("timeServiceSchedulerState")
}
