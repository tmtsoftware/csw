package csw.services.alarm.cli
import csw.services.alarm.cli.args.CommandLineArgs

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CommandExecutor(alarmAdminClient: AlarmAdminClient) {
  def execute(options: CommandLineArgs): Unit = {
    options.cmd match {
      case "init" ⇒ Await.result(alarmAdminClient.init(options), Duration.Inf)
    }
  }
}
