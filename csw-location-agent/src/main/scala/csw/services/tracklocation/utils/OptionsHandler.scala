package csw.services.tracklocation.utils

import com.typesafe.config.Config
import csw.services.tracklocation.models.Options

/**
 * Exposes utility methods to parse string, int, port options.
 */
final case class OptionsHandler(options: Options, appConfig: Option[Config]) {

  def stringOpt(entry: String, arg: Option[String] = None): Option[String] = {
    val value = if (arg.isDefined) { arg } else {
      appConfig.flatMap { c =>
        // XXX: Using only first name here
        val path = s"${options.names.head}.$entry"
        if (c.hasPath(path)) Some(c.getString(path)) else None
      }
    }
    if (value.isDefined) { value } else { None }
  }

  def intOpt(str: String, arg: Option[Int] = None): Option[Int] =
    stringOpt(str, arg.map(_.toString)).map(_.toInt)

  def portOpt(portKey: String, portValue: Option[Int]): Int =
    intOpt(portKey, portValue).getOrElse(Utils.getFreePort)
}
