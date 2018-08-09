package csw.services.alarm.cli
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.cli.args.CommandLineArgs
import csw.services.alarm.cli.utils.ConfigUtils
import csw.services.alarm.cli.wiring.ActorRuntime
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.location.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.concurrent.Future

class AlarmAdminClient(actorRuntime: ActorRuntime, locationService: LocationService, configUtils: ConfigUtils) {
  import actorRuntime._

  private val alarmServiceF: Future[AlarmAdminService] = new AlarmServiceFactory().adminApi(locationService)

  def init(args: CommandLineArgs): Future[Unit] = async {
    val config = await(configUtils.getConfig(args.isLocal, args.filePath, None))
    await(alarmServiceF).initAlarms(config, args.reset)
  }

  def severity(args: CommandLineArgs): Future[Unit] = alarmServiceF.map(_.setSeverity(args.alarmKey, args.severity))
}
