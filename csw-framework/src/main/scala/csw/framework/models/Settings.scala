/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.models

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}

class Settings(actorSystem: ActorSystem[SpawnProtocol.Command]) {
  private val crmConfig = actorSystem.settings.config.getConfig("csw-command-client.mini-crm")

  def startedSize: Int  = crmConfig.getInt("started-size")
  def responseSize: Int = crmConfig.getInt("response-size")
  def waiterSize: Int   = crmConfig.getInt("waiter-size")
}
