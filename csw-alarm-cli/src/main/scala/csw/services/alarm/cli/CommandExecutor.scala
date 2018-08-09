package csw.services.alarm.cli
import csw.services.alarm.cli.args.CommandLineArgs

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CommandExecutor(alarmAdminClient: AlarmAdminClient) {
  def execute(options: CommandLineArgs): Unit = {
    options.cmd match {
      case "init"   ⇒ await(alarmAdminClient.init(options))
      case "update" ⇒ await(alarmAdminClient.severity(options))
    }
  }

  def await[T](futureToAwait: Future[T]): T = Await.result(futureToAwait, Duration.Inf)
}
