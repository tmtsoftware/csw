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

  def init(options: CommandLineArgs): Future[Unit] = async {
    val config = await(configUtils.getConfig(options.isLocal, options.filePath, None))
    await(alarmServiceF).initAlarms(config, options.reset)
  }
}
