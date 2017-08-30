package csw.common.framework.models

import spray.json.JsonFormat

final case class ContainerInfo(name: String, locationServiceUsage: LocationServiceUsage, components: Set[ComponentInfo])

case object ContainerInfo {
  import spray.json.DefaultJsonProtocol._
  implicit val format: JsonFormat[ContainerInfo] = jsonFormat3(ContainerInfo.apply)
}
