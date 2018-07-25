package csw.services.alarm.client.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.services.alarm.api.exceptions.ConfigParseException
import csw.services.alarm.api.internal.AlarmRW
import csw.services.alarm.api.models.{AlarmMetadata, AlarmMetadataSet}
import csw.services.alarm.client.internal.configparser.ValidationResult.{Failure, Success}
import ujson.Js.Value
import upickle.default.{ReadWriter ⇒ RW, _}

/**
 * Parses the information represented in configuration files into respective models
 */
object ConfigParser extends AlarmRW {
  import SchemaRegistry._

  def parseAlarmMetadata(config: Config): AlarmMetadata       = parse[AlarmMetadata](config, ALARM_SCHEMA)
  def parseAlarmMetadataSet(config: Config): AlarmMetadataSet = parse[AlarmMetadataSet](config, ALARMS_SCHEMA)

  private def parse[T: RW](config: Config, schemaConfig: Config): T =
    ConfigValidator.validate(config, schemaConfig) match {
      case Success          ⇒ readJs[T](configToJsValue(config))
      case Failure(reasons) ⇒ throw ConfigParseException(reasons)
    }

  private def configToJsValue(config: Config): Value = ujson.read(config.root().render(ConfigRenderOptions.concise()))
}
