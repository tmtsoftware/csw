/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services

import org.apache.pekko.actor.CoordinatedShutdown
import caseapp.Command
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandsEntryPoint
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.services.cli.Commands
import csw.services.cli.Commands.StartOptions
import csw.services.internal.Wiring

import scala.util.control.NonFatal

object Main extends CommandsEntryPoint {
  def appName: String           = getClass.getSimpleName.dropRight(1) // remove $ from class name
  def appVersion: String        = BuildInfo.version
  override def progName: String = BuildInfo.name

  println(s"starting $progName-$appVersion")

  val StartCommand: Command[StartOptions] = new Command[StartOptions] {
    def run(options: StartOptions, args: RemainingArgs): Unit = {
      val wiring = new Wiring(options)
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

  override def commands: Seq[Command[?]] = List(StartCommand)
}
