package csw.event.client.perf.ocs.gateway.client

import csw.params.events.EventKey

sealed trait GatewayMessages

object GatewayMessages {

  sealed trait GatewayStreamRequest
  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None) extends GatewayStreamRequest

  sealed abstract class GatewayException(msg: String) extends RuntimeException(msg)
  case class InvalidComponent(msg: String)            extends GatewayException(msg)
  case class EmptyEventKeys()                         extends GatewayException("Request is missing event key")
  case class EventServerUnavailable()                 extends GatewayException("Event server is unavailable")
  case class InvalidMaxFrequency()                    extends GatewayException("Max frequency should be greater than zero")
  case class SetAlarmSeverityFailure(msg: String)     extends GatewayException(msg)

}