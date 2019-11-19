package csw.alarm.cli

import csw.alarm.cli.args.{ArgsParser, Options}
import csw.alarm.cli.wiring.Wiring
import csw.location.client.utils.LocationServerStatus

// $COVERAGE-OFF$
object Main extends App {
  private val name: String = BuildInfo.name

  new ArgsParser(name).parse(args.toList).foreach { options =>
    LocationServerStatus.requireUp(options.locationHost)
    run(options)
  }

  private def run(options: Options): Unit = {
    val wiring = Wiring.make(options.locationHost)
    import wiring._
    import actorRuntime._

    try {
      startLogging(name)
      cliApp.execute(options)
    } finally {
      shutdown()
    }
  }

}
// $COVERAGE-ON$
