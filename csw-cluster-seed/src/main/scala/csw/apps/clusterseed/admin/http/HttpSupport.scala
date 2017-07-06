package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Directive1, Directives}
import csw.apps.clusterseed.admin.internal.JsonSupport
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.logging.internal.LoggingLevels.Level

trait HttpSupport extends Directives with JsonSupport with ClusterSeedLogger.Simple {
  val logLevelParam: Directive1[Level] = parameter('value).map(Level(_))
  private def logRequest(req: HttpRequest): Unit =
    log.info("HTTP Request received",
      Map("url" → req.uri.toString(), "method" → req.method.value, "headers" → req.headers.mkString(",")))
  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))
}
