/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services

import org.apache.pekko.actor.CoordinatedShutdown
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.services.cli.Command
import csw.services.cli.Command.Start
import csw.services.internal.Wiring

import scala.util.control.NonFatal

object Main extends CommandApp[Command] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name
  println(s"starting $progName-$appVersion")

  override def run(command: Command, remainingArgs: RemainingArgs): Unit =
    command match {
      case s: Start => run(s)
    }

  def run(start: Start): Unit = {
    val wiring = new Wiring(start)
    import wiring._
    try {
      environment.setup()
      LoggingSystemFactory.start(appName, appVersion, settings.hostName, actorSystem)
      serviceList.foreach(_.start)
      CoordinatedShutdown(actorSystem).addJvmShutdownHook(shutdown())
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
        shutdown()
        exit(1)
    }
  }
}
