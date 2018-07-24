package csw.services.alarm.client.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.services.alarm.api.exceptions.ConfigParseException
import csw.services.alarm.api.internal.AlarmRW
import csw.services.alarm.api.models.AlarmMetadata
import csw.services.alarm.client.internal.configparser.ValidationResult.{Failure, Success}
import ujson.Js.Value
import upickle.default._

/**
 * Parses the information represented in configuration files into respective models
 */
object ConfigParser extends AlarmRW {
  import SchemaRegistry._

  def parseAlarmMetadata(config: Config): AlarmMetadata =
    AscfValidator.validate(config, ALARM_SCHEMA) match {
      case Success          ⇒ readJs[AlarmMetadata](configToJsValue(config))
      case Failure(reasons) ⇒ throw ConfigParseException(reasons)
    }

  def parseAlarmsMetadata(config: Config): List[AlarmMetadata] =
    AscfValidator.validate(config, ALARMS_SCHEMA) match {
      case Success          ⇒ readJs[List[AlarmMetadata]](configToJsValue(config)("alarms"))
      case Failure(reasons) ⇒ throw ConfigParseException(reasons)
    }

  private def configToJsValue(config: Config): Value = ujson.read(config.root().render(ConfigRenderOptions.concise()))
}
