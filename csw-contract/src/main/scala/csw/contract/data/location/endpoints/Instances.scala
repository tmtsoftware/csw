package csw.contract.data.location.endpoints

import csw.contract.data.location.models.Instances._
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.Endpoint
import csw.location.api.codec.LocationServiceCodecs
import msocket.api.codecs.BasicCodecs

object Instances extends LocationServiceCodecs with BasicCodecs {

  val endpoints: Map[String, Endpoint] = Map(
    "register" -> Endpoint(
      requests = List(registerAkka, registerHttp),
      responses = List(registrationFailed, akkaLocation)
    ),
    "unregister" -> Endpoint(
      requests = List(unregister),
      responses = List(done)
    ),
    "unregisterAll" -> Endpoint(
      requests = List(),
      responses = List(done)
    ),
    "find" -> Endpoint(
      requests = List(find),
      responses = List()
    ),
    "resolve" -> Endpoint(
      requests = List(resolve),
      responses = List()
    ),
    "listEntries" -> Endpoint(
      requests = List(),
      responses = List()
    ),
    "listByComponentType" -> Endpoint(
      requests = List(listByComponentType),
      responses = List()
    ),
    "listByHostname" -> Endpoint(
      requests = List(listByHostname),
      responses = List()
    ),
    "listByConnectionType" -> Endpoint(
      requests = List(listByConnectionType),
      responses = List()
    ),
    "listByPrefix" -> Endpoint(
      requests = List(listByPrefix),
      responses = List()
    ),
    "track" -> Endpoint(
      requests = List(track),
      responses = List(locationRemoved, locationUpdated)
    )
  )
}
