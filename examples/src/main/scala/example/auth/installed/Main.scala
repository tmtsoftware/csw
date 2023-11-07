/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.auth.installed

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import csw.aas.installed.api.InstalledAppAuthAdapter
import csw.location.client.utils.LocationServerStatus
import example.auth.installed.commands.{AppCommand, CommandFactory}

// #main-app
object Main {

  def main(args: Array[String]): Unit = {

    LocationServerStatus.requireUpLocally()

    implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "example-system")

    val adapter: InstalledAppAuthAdapter = AdapterFactory.makeAdapter

    val command: Option[AppCommand] = CommandFactory.makeCommand(adapter, args)

    try {
      command.foreach(_.run())
    }
    finally {
      actorSystem.terminate()
    }
  }
}
// #main-app
