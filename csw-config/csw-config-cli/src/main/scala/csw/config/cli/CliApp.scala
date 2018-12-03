package csw.config.cli
import csw.config.cli.args.{ArgsParser, Options}

/**
 * Application object allowing program execution from command line.
 */
// $COVERAGE-OFF$
class CliApp(commandLineRunner: CommandLineRunner) {
  def start(name: String, args: Array[String]): Unit =
    new ArgsParser(name).parse(args).foreach { options ⇒
      start(options)
    }

  def start(options: Options): Any = {
    options.op match {
      case "login"  ⇒ commandLineRunner.login()
      case "logout" ⇒ commandLineRunner.logout()
      //adminApi
      case "create"             ⇒ commandLineRunner.create(options)
      case "update"             ⇒ commandLineRunner.update(options)
      case "get"                ⇒ commandLineRunner.get(options)
      case "delete"             ⇒ commandLineRunner.delete(options)
      case "list"               ⇒ commandLineRunner.list(options)
      case "history"            ⇒ commandLineRunner.history(options)
      case "historyActive"      ⇒ commandLineRunner.historyActive(options)
      case "setActiveVersion"   ⇒ commandLineRunner.setActiveVersion(options)
      case "resetActiveVersion" ⇒ commandLineRunner.resetActiveVersion(options)
      case "getActiveVersion"   ⇒ commandLineRunner.getActiveVersion(options)
      case "getActiveByTime"    ⇒ commandLineRunner.getActiveByTime(options)
      case "getMetadata"        ⇒ commandLineRunner.getMetadata(options)
      //clientApi
      case "exists"    ⇒ commandLineRunner.exists(options)
      case "getActive" ⇒ commandLineRunner.getActive(options)
      case x           ⇒ throw new RuntimeException(s"Unknown operation: $x")
    }
  }
}
// $COVERAGE-ON$
