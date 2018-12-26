package csw.common.components.framework

import csw.params.commands.CommandName
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Subsystem.NFIRAOS
import csw.params.core.models.{Choice, Choices, Prefix}
import csw.alarm.api.models.AlarmSeverity.Warning
import csw.alarm.api.models.Key.AlarmKey
import csw.params.core.states.StateName

object SampleComponentState {
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
  val prefix                    = Prefix("wfos.prog.cloudcover")
  val successPrefix             = Prefix("wfos.prog.cloudcover.success")
  val failedPrefix              = Prefix("wfos.prog.cloudcover.failure")

  val setSeverityCommand = CommandName("alarm.setSeverity.success")
  val testAlarmKey       = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
  val testSeverity       = Warning

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
  val choiceKey: GChoiceKey          = ChoiceKey.make("choiceKey", choices)
  val httpConnection: HttpConnection = HttpConnection(ComponentId("exampleHTTPService", ComponentType.Service))
  val tcpConnection: TcpConnection   = TcpConnection(ComponentId("exampleTcpService", ComponentType.Service))

  // States
  val timeServiceSchedulerState = StateName("timeServiceSchedulerState")
}
