/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.agent

import csw.location.agent.args.ArgsParser
import csw.location.agent.commons.LocationAgentLogger
import csw.location.agent.models.Command
import csw.location.agent.wiring.Wiring
import csw.location.client.utils.LocationServerStatus

/**
 * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
 */
object Main {
  private val name = BuildInfo.name
  private val log  = LocationAgentLogger.getLogger

  def main(args: Array[String]): Unit = start(args, startLogging = true)

  def start(args: Array[String], startLogging: Boolean = false): Option[(Process, Wiring)] =
    new ArgsParser(name).parse(args.toList).map { options =>
      LocationServerStatus.requireUpLocally()

      val wiring = new Wiring
      if (startLogging) wiring.actorRuntime.startLogging(name)

      val command = Command.parse(options)
      log.info(s"commandText: ${command.commandText}, command: ${command.toString}")

      val locationAgent = new LocationAgent(options.prefixes, command, options.networkType, wiring, options.agentPrefix)
      val process       = locationAgent.run()
      (process, wiring)
    }
}
