package csw.services.alarm.client.internal.configparser

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.services.alarm.api.exceptions.ConfigParseException
import csw.services.alarm.api.internal.AlarmRW
import csw.services.alarm.api.internal.ValidationResult.{Failure, Success}
import csw.services.alarm.api.models.AlarmMetadataSet
import ujson.Js.Value
import upickle.default.{ReadWriter ⇒ RW, _}

/**
 * Parses the information represented in configuration files into respective models
 */
object ConfigParser extends AlarmRW {
  val ALARMS_SCHEMA: Config = ConfigFactory.parseResources("alarms-schema.conf")

  def parseAlarmMetadataSet(config: Config): AlarmMetadataSet = parse[AlarmMetadataSet](config)

  private def parse[T: RW](config: Config): T =
    ConfigValidator.validate(config, ALARMS_SCHEMA) match {
      case Success          ⇒ readJs[T](configToJsValue(config))
      case Failure(reasons) ⇒ throw ConfigParseException(reasons)
    }

  private def configToJsValue(config: Config): Value = ujson.read(config.root().render(ConfigRenderOptions.concise()))
}
