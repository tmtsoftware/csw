package csw.location.client.utils

import csw.location.client.internal.Settings
import csw.network.utils.SocketUtils

import scala.concurrent.duration.{Duration, DurationDouble}

object LocationServerStatus {

  final def requireUp(locationHost: String, _within: Duration = 5.seconds): Unit =
    SocketUtils.requireServerUp(
      host = locationHost,
      port = Settings().serverPort,
      within = _within,
      msg = s"Location server is not running at $locationHost:7654. Please check online documentation for location server setup"
    )

  final def requireUpLocally(within: Duration = 5.seconds): Unit = requireUp("localhost", within)

}
