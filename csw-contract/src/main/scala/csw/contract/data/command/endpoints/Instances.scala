package csw.contract.data.command.endpoints

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.contract.data.command.models.Instances._
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.Endpoint

import scala.concurrent.duration.FiniteDuration

object Instances extends CommandServiceCodecs {

  private val observeValidate: CommandServiceHttpMessage     = Validate(observe)
  private val setupValidate: CommandServiceHttpMessage       = Validate(setup)
  private val observeSubmit: CommandServiceHttpMessage       = Submit(observe)
  private val setupSubmit: CommandServiceHttpMessage         = Submit(setup)
  private val observeOneway: CommandServiceHttpMessage       = Oneway(observe)
  private val setupOneway: CommandServiceHttpMessage         = Oneway(setup)
  private val setupQuery: CommandServiceHttpMessage          = Query(id)
  private val seconds                                        = 30
  private val queryFinal: CommandServiceWebsocketMessage     = QueryFinal(id, Timeout(FiniteDuration(seconds, TimeUnit.SECONDS)))
  private val subscribeState: CommandServiceWebsocketMessage = SubscribeCurrentState(states)

  val endpoints: Map[String, Endpoint] = Map(
    "validate" -> Endpoint(
      requests = List(observeValidate, setupValidate),
      responses = List(accepted, invalid)
    ),
    "submit" -> Endpoint(
      requests = List(observeSubmit, setupSubmit),
      responses = List(accepted, cancelled)
    ),
    "oneWay" -> Endpoint(
      requests = List(observeOneway, setupOneway),
      responses = List(accepted, invalid)
    ),
    "query" -> Endpoint(
      requests = List(setupQuery),
      responses = List(accepted, cancelled)
    ),
    "queryFinal" -> Endpoint(
      requests = List(queryFinal, setupValidate),
      responses = List(started, accepted)
    ),
    "subscribeCurrentState" -> Endpoint(
      requests = List(subscribeState),
      responses = List(currentState)
    )
  )
}
