package csw.contract.data.command

import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers.name
import csw.contract.generator._
import csw.params.commands.CommandResponse._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Units
import csw.params.core.states.CurrentState

object CommandContract extends CommandData with CommandServiceCodecs {
  private val models: ModelSet = ModelSet.models(
    ModelType(observe, setup),
    ModelType(commandName),
    ModelType[Parameter[_]](paramSet.toList),
    ModelType(KeyType),
    ModelType(Units),
    ModelType(result),
    ModelType[SubmitResponse](cancelled, completed, error, invalid, locked, started),
    ModelType[OnewayResponse](accepted, invalid, locked),
    ModelType[ValidateResponse](accepted, invalid, locked),
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
      hcdBusyIssue,
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

  private val httpRequests = new RequestSet[CommandServiceHttpMessage] {
    requestType(observeValidate)
    requestType(observeSubmit)
    requestType(setupQuery)
    requestType(observeOneway)
  }

  private val websocketRequests = new RequestSet[CommandServiceWebsocketMessage] {
    requestType(queryFinal)
    requestType(subscribeState)
  }

  private val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[Validate], name[ValidateResponse]),
    Endpoint(name[Submit], name[SubmitResponse]),
    Endpoint(name[Query], name[SubmitResponse]),
    Endpoint(name[Oneway], name[OnewayResponse])
  )

  private val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[QueryFinal], name[SubmitResponse]),
    Endpoint(name[SubscribeCurrentState], name[CurrentState])
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("command-service/README.md"))

  val service: Service = Service(
    Contract(httpEndpoints, httpRequests),
    Contract(webSocketEndpoints, websocketRequests),
    models,
    readme
  )
}
