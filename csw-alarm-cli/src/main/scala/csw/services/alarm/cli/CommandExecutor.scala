package csw.services.alarm.cli
import csw.services.alarm.cli.args.Options

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CommandExecutor(alarmAdminClient: AlarmAdminClient) {
  def execute(options: Options): Any = {
    options.cmd match {
      case "init"          ⇒ await(alarmAdminClient.init(options))
      case "list"          ⇒ await(alarmAdminClient.list(options))
      case "status"        ⇒ await(alarmAdminClient.status(options))
      case "acknowledge"   ⇒ await(alarmAdminClient.acknowledge(options))
      case "unacknowledge" ⇒ await(alarmAdminClient.unacknowledge(options))
      case "activate"      ⇒ await(alarmAdminClient.activate(options))
      case "deactivate"    ⇒ await(alarmAdminClient.deactivate(options))
      case "shelve"        ⇒ await(alarmAdminClient.shelve(options))
      case "unshelve"      ⇒ await(alarmAdminClient.unshelve(options))
      case "reset"         ⇒ await(alarmAdminClient.reset(options))

      case "severity" ⇒
        options.subCmd match {
          case "get"       ⇒ await(alarmAdminClient.getSeverity(options))
          case "set"       ⇒ await(alarmAdminClient.setSeverity(options))
          case "subscribe" ⇒ await(alarmAdminClient.subscribeSeverity(options))
        }

      case "health" ⇒
        options.subCmd match {
          case "get"       ⇒ await(alarmAdminClient.getHealth(options))
          case "subscribe" ⇒ await(alarmAdminClient.subscribeHealth(options))
        }
    }
  }

  def await[T](futureToAwait: Future[T]): T = Await.result(futureToAwait, Duration.Inf)
}
