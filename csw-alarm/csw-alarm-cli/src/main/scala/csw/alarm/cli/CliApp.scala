package csw.alarm.cli
import csw.alarm.cli.args.Options

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CliApp(alarmAdminClient: CommandLineRunner) {
  def execute(options: Options): Unit =
    try {
      options.cmd match {
        case "init"          ⇒ await(alarmAdminClient.init(options))
        case "list"          ⇒ await(alarmAdminClient.list(options))
        case "acknowledge"   ⇒ await(alarmAdminClient.acknowledge(options))
        case "unacknowledge" ⇒ await(alarmAdminClient.unacknowledge(options))
        case "activate"      ⇒ await(alarmAdminClient.activate(options))
        case "deactivate"    ⇒ await(alarmAdminClient.deactivate(options))
        case "shelve"        ⇒ await(alarmAdminClient.shelve(options))
        case "unshelve"      ⇒ await(alarmAdminClient.unshelve(options))
        case "reset"         ⇒ await(alarmAdminClient.reset(options))

        case "severity" ⇒
          options.subCmd match {
            case "get"                        ⇒ await(alarmAdminClient.getSeverity(options))
            case "set" if options.autoRefresh ⇒ await({ alarmAdminClient.refreshSeverity(options); Future.never })
            case "set"                        ⇒ await(alarmAdminClient.setSeverity(options))
            case "subscribe"                  ⇒ await { val (_, doneF) = alarmAdminClient.subscribeSeverity(options); doneF }
          }

        case "health" ⇒
          options.subCmd match {
            case "get"       ⇒ await(alarmAdminClient.getHealth(options))
            case "subscribe" ⇒ await { val (_, doneF) = alarmAdminClient.subscribeHealth(options); doneF }
          }
      }
    } catch {
      case ex: Exception => println(ex.getMessage)
    }

  def await[T](futureToAwait: Future[T]): T = Await.result(futureToAwait, Duration.Inf)
}
