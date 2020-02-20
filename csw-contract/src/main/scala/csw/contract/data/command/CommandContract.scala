package csw.contract.data.command

import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.contract.generator.ClassNameHelpers.name
import csw.contract.generator._
import csw.params.commands.CommandResponse._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Units
import csw.params.core.states.CurrentState
import io.bullet.borer.Encoder

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

  implicit def httpEnc[Sub <: CommandServiceHttpMessage]: Encoder[Sub]           = SubTypeCodec.encoder(httpCodecsValue)
  implicit def websocketEnc[Sub <: CommandServiceWebsocketMessage]: Encoder[Sub] = SubTypeCodec.encoder(websocketCodecs)

  val httpRequests: ModelSet = ModelSet(
    ModelType(observeValidate),
    ModelType(observeSubmit),
    ModelType(setupQuery),
    ModelType(observeOneway)
  )

  val websocketRequests: ModelSet = ModelSet(
    ModelType(queryFinal),
    ModelType(subscribeState)
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

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract(webSocketEndpoints, websocketRequests),
    models = models
  )
}
