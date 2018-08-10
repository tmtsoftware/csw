package csw.services.alarm.api.scaladsl
import com.typesafe.config.Config
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmMetadata, Key}

import scala.concurrent.Future

trait MetadataService {
  def initAlarms(inputConfig: Config, reset: Boolean = false): Future[Unit]

  def getMetadata(key: AlarmKey): Future[AlarmMetadata]
  def getMetadata(key: Key): Future[List[AlarmMetadata]]

  def activate(key: AlarmKey): Future[Unit]   // api only for test purpose
  def deActivate(key: AlarmKey): Future[Unit] // api only for test purpose
}
