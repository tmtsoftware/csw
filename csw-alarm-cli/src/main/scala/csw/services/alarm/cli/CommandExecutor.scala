package csw.services.alarm.cli
import csw.services.alarm.cli.args.Options

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CommandExecutor(alarmAdminClient: AlarmAdminClient) {
  def execute(options: Options): Unit = {
    options.cmd match {
      case "init"        ⇒ await(alarmAdminClient.init(options))
      case "update"      ⇒ await(alarmAdminClient.severity(options))
      case "acknowledge" ⇒ await(alarmAdminClient.acknowledge(options))
      case "activate"    ⇒ await(alarmAdminClient.activate(options))
      case "deactivate"  ⇒ await(alarmAdminClient.deactivate(options))
      case "shelve"      ⇒ await(alarmAdminClient.shelve(options))
      case "unshelve"    ⇒ await(alarmAdminClient.unshelve(options))
      case "reset"       ⇒ await(alarmAdminClient.reset(options))
      case "list"        ⇒ await(alarmAdminClient.list(options))
    }
  }

  def await[T](futureToAwait: Future[T]): T = Await.result(futureToAwait, Duration.Inf)
}
