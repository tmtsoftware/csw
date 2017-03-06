package csw.services.tracklocation.utils

import java.net.ServerSocket

import com.typesafe.config.Config
import csw.services.tracklocation.models.Options

final case class OptionsHandler(val options: Options, val appConfig: Option[Config]) {

  def stringOpt(opt: String, arg: Option[String] = None): Option[String] = {
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

  def intOpt(str: String, arg: Option[Int] = None): Option[Int] ={
    stringOpt(str, arg.map(_.toString)).map(_.toInt)
  }

  def portOpt(portKey:String, portValue: Option[Int]): Int ={
    intOpt(portKey, portValue).getOrElse(getFreePort)
  }

  // Find a random, free port to use
  def getFreePort: Int = {
    val sock = new ServerSocket(0)
    val port = sock.getLocalPort
    sock.close()
    port
  }
}