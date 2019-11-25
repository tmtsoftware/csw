package csw.command.client.internal

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[command] object Timeouts {
  val DefaultTimeout: FiniteDuration = 30.seconds
}
