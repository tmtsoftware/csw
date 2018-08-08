package csw.services.alarm.cli
import csw.services.BuildInfo
import csw.services.alarm.cli.args.ArgsParser

// $COVERAGE-OFF$
object Main extends App {
  new ArgsParser(BuildInfo.name)
}
// $COVERAGE-ON$
