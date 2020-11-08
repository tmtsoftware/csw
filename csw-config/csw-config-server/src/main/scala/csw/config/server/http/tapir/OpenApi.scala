package csw.config.server.http.tapir

import csw.config.server.http.tapir.Endpoints._
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._

object OpenApi extends App {
  val docs = List(getConfigEndpoint, existEndpoint, createConfigEndpoint).toOpenAPI("Config Service", "1.0")
  println(docs.toYaml)
}
