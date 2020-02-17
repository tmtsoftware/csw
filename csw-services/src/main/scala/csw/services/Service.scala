package csw.services

import csw.services.utils.ColoredConsole.{GREEN, RED}

import scala.util.control.NonFatal

object Service {

  def start[T](name: String, service: => T): T = {
    GREEN.println(s"Starting $name ...")
    try {
      val result = service
      GREEN.println(s"Successfully started $name.")
      result
    }
    catch {
      case NonFatal(e) =>
        RED.println(e.getMessage)
        RED.println(s"Failed to start $name!")
        throw e
    }
  }

}
