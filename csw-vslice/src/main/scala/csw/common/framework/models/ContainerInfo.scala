package csw.common.framework.models

import spray.json.JsonFormat

final case class ContainerInfo(name: String, components: Set[ComponentInfo])

case object ContainerInfo {
  import spray.json.DefaultJsonProtocol._
  implicit val format: JsonFormat[ContainerInfo] = jsonFormat2(ContainerInfo.apply)
}
