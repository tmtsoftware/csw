package csw.contract.data.command

import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.contract.generator.ClassNameHelpers.name
import csw.contract.generator._
import csw.params.commands.CommandResponse._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Units
import csw.params.core.states.CurrentState

object CommandContract extends CommandData with CommandServiceCodecs {
  val models: ModelSet = ModelSet(
    ModelType(observe, setup),
    ModelType(commandName),
    ModelType[Parameter[_]](
      intParameter,
      arrayParameter,
      structParameter,
      matrixParameter,
      coordsParameter,
      utcTimeParam,
      raDecParameter,
      choiceParameter
    ),
    ModelType(KeyType),
    ModelType(Units),
    ModelType(result),
    ModelType(cancelled, completed, error, invalid, locked, started),
    ModelType(accepted, invalid, locked),
    ModelType(accepted, invalid, locked),
    ModelType(currentState),
    ModelType(
      assemblyBusyIssue,
      idNotAvailableIssue,
      missingKeyIssue,
      parameterValueOutOfRangeIssue,
      requiredAssemblyUnavailableIssue,
      requiredSequencerUnavailableIssue,
      requiredServiceUnavailableIssue,
      requiredHCDUnavailableIssue,
      unresolvedLocationsIssue,
      unsupportedCommandInStateIssue,
      unsupportedCommandIssue,
      wrongInternalStateIssue,
      wrongNumberOfParametersIssue,
      wrongParameterTypeIssue,
      wrongPrefixIssue,
      wrongUnitsIssue,
      otherIssue
    )
  )

  val httpRequests: Map[String, ModelType[_]] = Map(
    name[Validate] -> ModelType(observeValidate),
    name[Submit]   -> ModelType(observeSubmit),
    name[Query]    -> ModelType(setupQuery),
    name[Oneway]   -> ModelType(observeOneway)
  )

  val websocketRequests: Map[String, ModelType[_]] = Map(
    name[QueryFinal]            -> ModelType(queryFinal),
    name[SubscribeCurrentState] -> ModelType(subscribeState)
  )

  val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[Validate], name[ValidateResponse]),
    Endpoint(name[Submit], name[SubmitResponse]),
    Endpoint(name[Query], name[SubmitResponse]),
    Endpoint(name[Oneway], name[OnewayResponse])
  )

  val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[QueryFinal], name[SubmitResponse]),
    Endpoint(name[SubscribeCurrentState], name[CurrentState])
  )

  val httpContract: Contract = Contract(httpEndpoints, httpRequests)

  val webSocketContract: Contract = Contract(webSocketEndpoints, websocketRequests)

  val service: Service = Service(httpContract, webSocketContract, models)
}
