package csw.location.agent.utils

import com.typesafe.config.Config
import csw.location.agent.args.Options
import csw.network.utils.SocketUtils

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

  private def intOpt(str: String, arg: Option[Int]): Option[Int] =
    stringOpt(str, arg.map(_.toString)).map(_.toInt)

  def portOpt(portKey: String, portValue: Option[Int]): Int =
    intOpt(portKey, portValue).getOrElse(SocketUtils.getFreePort)
}
