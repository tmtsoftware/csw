package csw.services.tracklocation.utils

import com.typesafe.config.Config
import csw.services.tracklocation.models.Options

object StringOptHandler {
  def apply(opt: String, arg: Option[String] = None, options: Options, appConfig: Option[Config]): Option[String] = {
    val value = if (arg.isDefined) { arg }
    else {
      appConfig.flatMap { c =>
        // XXX: Using only first name here
        val path = s"${options.names.head}.$opt"
        if (c.hasPath(path)) Some(c.getString(path)) else None
      }
    }
    if (value.isDefined) { value } else { None }
  }
}
