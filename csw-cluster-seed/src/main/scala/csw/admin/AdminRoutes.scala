package csw.admin

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.Materializer

class AdminRoutes(adminExceptionHandler: AdminExceptionHandler,
                  logAdmin: LogAdmin)(implicit val materializer: Materializer) {

  val logLevelParam: Directive1[String] = parameter('value)

  val route: Route = handleExceptions(adminExceptionHandler.exceptionHandler) {
    path("admin" / "logging" / Segment / "level") { componentName ⇒
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Say hello to $componentName</h1>"))
      } ~
      post {
        logLevelParam { (logLevel) ⇒
          //complete(StatusCodes.Created -> configService.create(filePath, configData, annex, comment))
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }
    }
  }
}
