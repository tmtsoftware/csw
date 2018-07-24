package csw.services.alarm.client.internal.configparser
import com.typesafe.config.{Config, ConfigFactory}

object SchemaRegistry {

  val ALARM_SCHEMA: Config  = ConfigFactory.parseResources("alarm-schema.conf")
  val ALARMS_SCHEMA: Config = ConfigFactory.parseResources("alarms-schema.conf")

}
