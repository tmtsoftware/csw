/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.cli

import csw.config.cli.args.{ArgsParser, Options}
import csw.config.cli.wiring.Wiring
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

// $COVERAGE-OFF$
object Main {
  def main(args: Array[String]): Unit = {
    val name = BuildInfo.name

    new ArgsParser(name).parse(args.toList).foreach(run)

    def run(options: Options): Unit = {
      LocationServerStatus.requireUp(options.locationHost)

      val wiring = Wiring.make(options.locationHost)
      import wiring._
      LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, actorSystem)

      try {
        cliApp.start(options)
      }
      finally {
        actorRuntime.shutdown()
      }
    }
  }
}
// $COVERAGE-ON$
