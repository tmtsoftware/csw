package csw.location.agent.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

private[location] object LocationAgentLogger extends LoggerFactory(Prefix(CSW, "location_agent_app"))
