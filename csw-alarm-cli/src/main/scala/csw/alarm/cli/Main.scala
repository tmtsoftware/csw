package csw.alarm.cli

import akka.http.scaladsl.Http
import csw.alarm.cli.args.{ArgsParser, Options}
import csw.alarm.cli.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.alarm.cli.wiring.Wiring
import csw.location.client.utils.LocationServerStatus
import csw.services.BuildInfo

// $COVERAGE-OFF$
object Main extends App {
  private val name: String = BuildInfo.name

  new ArgsParser(name).parse(args).foreach { options =>
    LocationServerStatus.requireUp(options.locationHost)
    run(options)
  }

  private def run(options: Options): Unit = {
    val wiring = Wiring.make(options.locationHost)
    import wiring._
    import actorRuntime._
    startLogging(name)

    try commandExecutor.execute(options)
    finally Http().shutdownAllConnectionPools().onComplete(_ ⇒ shutdown(ApplicationFinishedReason))
  }

}
// $COVERAGE-ON$
