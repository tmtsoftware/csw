package csw.apps.clusterseed.admin.internal

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import csw.services.logging.models.LoggerMetadata
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val configMetadataFormat: RootJsonFormat[LoggerMetadata] = jsonFormat1(LoggerMetadata.apply)

}
