/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli

import csw.event.cli.args.{ArgsParser, Options}
import csw.event.cli.wiring.Wiring
import csw.location.client.utils.LocationServerStatus

// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  new ArgsParser(name).parse(args.toList).foreach { options =>
    LocationServerStatus.requireUp(options.locationHost)
    run(options)
  }

  private def run(options: Options): Unit = {
    val wiring = Wiring.make(options.locationHost)
    import wiring._
    val runtime = actorRuntime
    import runtime._
    startLogging(name)

    try cliApp.start(options)
    finally actorRuntime.shutdown()
  }
}
// $COVERAGE-ON$
