package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Directive1, Directives, MalformedQueryParamRejection}
import csw.apps.clusterseed.admin.internal.JsonSupport
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level

trait HttpSupport extends Directives with JsonSupport with ClusterSeedLogger.Simple {
  val logLevelParam: Directive1[Level] = parameter('value).flatMap {
    case value if Level.hasLevel(value) => provide(Level(value))
    case _ =>
      reject(MalformedQueryParamRejection("value", s"Supported logging levels are [${LoggingLevels.stringify()}]"))
  }

  private def logRequest(req: HttpRequest): Unit =
    log.info("HTTP Request received",
             Map("url" → req.uri.toString(), "method" → req.method.value, "headers" → req.headers.mkString(",")))
  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))
}
