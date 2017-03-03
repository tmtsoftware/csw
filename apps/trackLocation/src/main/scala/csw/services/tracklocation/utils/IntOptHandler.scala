package csw.services.tracklocation.utils

import com.typesafe.config.Config
import csw.services.tracklocation.models.Options

object IntOptHandler {
   def apply(str: String, arg: Option[Int] = None, options: Options, appConfig: Option[Config]): Option[Int] ={
     StringOptHandler(str, arg.map(_.toString), options, appConfig).map(_.toInt)
  }
}