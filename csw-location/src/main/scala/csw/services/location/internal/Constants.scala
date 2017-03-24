package csw.services.location.internal

import akka.cluster.ddata.LWWMapKey
import csw.services.location.models.{Connection, Location}

object Constants {
  val PathKey = "path"
  val ActorPathKey = "actor-path"
  val PrefixKey = "prefix"

  val RegistryKey: LWWMapKey[Connection, Location] = LWWMapKey[Connection, Location]("location-service-registry")
}
