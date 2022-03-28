/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.common.components.framework

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.AlarmSeverity.Warning
import csw.alarm.models.Key.AlarmKey
import csw.location.api.models
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandName
import csw.params.core.generics.KeyType.{ChoiceKey, StringKey}
import csw.params.core.generics.{GChoiceKey, Parameter}
import csw.params.core.models.{Choice, Choices}
import csw.params.core.states.StateName
import csw.params.events.EventName
import csw.prefix.models.Subsystem.NFIRAOS
import csw.prefix.models.{Prefix, Subsystem}

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

  val setSeverityCommand          = CommandName("alarm.setSeverity.success")
  val testAlarmKey                = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")
  val testSeverity: AlarmSeverity = Warning

  val diagnosticDataEventName                = EventName("diagnostic-data")
  val diagnosticModeParam: Parameter[String] = StringKey.make("diagnostic-data").set("diagnostic-publish")

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
  val httpConnection: HttpConnection = HttpConnection(
    ComponentId(Prefix(Subsystem.CSW, "exampleHTTPService"), ComponentType.Service)
  )
  val tcpConnection: TcpConnection = TcpConnection(
    models.ComponentId(Prefix(Subsystem.CSW, "exampleTcpService"), ComponentType.Service)
  )

  // States
  val timeServiceSchedulerState = StateName("timeServiceSchedulerState")
}
