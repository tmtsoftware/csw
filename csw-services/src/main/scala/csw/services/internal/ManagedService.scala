package csw.services.internal

import csw.services.utils.ColoredConsole.{GREEN, RED}

import scala.util.control.NonFatal

case class ManagedService[T, U](
    serviceName: String,
    enable: Boolean,
    private val _start: () => T,
    private val _stop: T => U
) {
  private var startResult: Option[T] = None

  def start: Option[T] = {
    if (enable) {
      try {
        GREEN.println(s"Starting $serviceName ...")
        startResult = Some(_start())
        GREEN.println(s"Successfully started $serviceName.")
      }
      catch {
        case NonFatal(e) =>
          RED.println(e.getMessage)
          RED.println(s"Failed to start $serviceName!")
          throw e
      }
    }
    startResult
  }

  def stop: Option[U] = startResult.map(_stop)
}
