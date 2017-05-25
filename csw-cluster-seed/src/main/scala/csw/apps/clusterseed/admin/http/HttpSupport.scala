package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.server.{Directive1, Directives}
import csw.services.logging.internal.LoggingLevels.Level

trait HttpSupport extends Directives {
  val logLevelParam: Directive1[Level] = parameter('value).map(Level(_))
}
