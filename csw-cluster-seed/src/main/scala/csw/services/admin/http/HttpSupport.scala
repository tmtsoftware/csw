package csw.services.admin.http

import akka.http.scaladsl.server.{Directive1, Directives}
import csw.services.admin.internal.JsonSupport
import csw.services.logging.internal.LoggingLevels.Level

trait HttpSupport extends Directives with JsonSupport {
  val logLevelParam: Directive1[Level] = parameter('value).map(Level(_))
}
