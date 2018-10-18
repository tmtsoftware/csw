package csw.event.cli

import akka.http.scaladsl.Http
import csw.event.cli.args.{ArgsParser, Options}
import csw.event.cli.commons.ApplicationFinishedReason
import csw.event.cli.wiring.Wiring
import csw.location.client.utils.LocationServerStatus
import csw.services.BuildInfo

// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  new ArgsParser(name).parse(args).foreach { options =>
    LocationServerStatus.requireUp(options.locationHost)
    run(options)
  }

  private def run(options: Options): Unit = {
    val wiring = Wiring.make(options.locationHost)
    import wiring._
    import actorRuntime._
    startLogging(name)

    try cliApp.start(options)
    finally Http().shutdownAllConnectionPools().onComplete(_ ⇒ actorRuntime.shutdown(ApplicationFinishedReason))
  }
}
// $COVERAGE-ON$
