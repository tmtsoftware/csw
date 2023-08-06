/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.generator
import java.io.File
import java.net.URI
import java.nio.file.{Files, Path, Paths}
import csw.contract.ResourceFetcher
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationRequest.{Register, Unregister}
import csw.location.api.messages.LocationStreamRequest.Track
import csw.location.api.messages.{LocationRequest, LocationStreamRequest}
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models.*
import csw.prefix.models.Prefix
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.Comparator

class FilesGeneratorTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with LocationServiceCodecs {

  override def afterAll(): Unit = {
    val path = Paths.get("csw-contract/src/test/testOutput")
    if (path.toFile.exists())
      Files
        .walk(path)
        .sorted(Comparator.reverseOrder())
        .map(_.toFile)
        .forEach(_.delete)
  }

  test("should generate samples for given services") {
    val uri                                  = new URI("some_path")
    val locationService                      = "location-service"
    val metadata: Metadata                   = Metadata().add("key1", "value")
    val componentId: ComponentId             = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
    val pekkoConnection: PekkoConnection     = PekkoConnection(componentId)
    val httpConnection: HttpConnection       = HttpConnection(componentId)
    val pekkoRegistration: PekkoRegistration = PekkoRegistration(pekkoConnection, uri, metadata)
    val httpRegistration: HttpRegistration   = HttpRegistration(httpConnection, 2090, "somePath")
    val pekkoLocation: Location              = PekkoLocation(pekkoConnection, uri, metadata)
    val httpLocation: Location               = HttpLocation(httpConnection, uri, Metadata.empty)
    val locationUpdated: TrackingEvent       = LocationUpdated(pekkoLocation)
    val pekkoRegister: Register              = Register(pekkoRegistration)
    val httpRegister: Register               = Register(httpRegistration)
    val unregister: Unregister               = Unregister(httpConnection)
    val track: Track                         = Track(pekkoConnection)
    val httpEndpoints: List[Endpoint] = List(
      Endpoint(
        requestType = "Register",
        responseType = "Location",
        errorTypes = List("RegistrationFailed", "OtherLocationIsRegistered")
      )
    )
    val websocketEndpoints: List[Endpoint] = List(
      Endpoint(
        requestType = "Track",
        responseType = "TrackingEvent",
        errorTypes = List("ServiceError")
      )
    )
    val models: ModelSet = ModelSet.models(
      ModelType(pekkoLocation, httpLocation),
      ModelType(locationUpdated)
    )
    val httpRequests = new RequestSet[LocationRequest] {
      requestType(pekkoRegister, httpRegister)
      requestType(unregister)
    }
    val webSocketRequests = new RequestSet[LocationStreamRequest] {
      requestType(track)
    }
    val services: Services = Services(
      Map(
        locationService -> Service(
          `http-contract` = Contract(httpEndpoints, httpRequests),
          `websocket-contract` = Contract(websocketEndpoints, webSocketRequests),
          models,
          Readme(ResourceFetcher.getResourceAsString(locationService + "/README.md"))
        )
      )
    )

    val testOutput = "csw-contract/src/test/testOutput"
    FilesGenerator.generate(services, testOutput)

    shouldMatchFile(s"$testOutput/$locationService/http-contract.json", locationService + "/testHttpContract.json")
    shouldMatchFile(
      s"$testOutput/$locationService/websocket-contract.json",
      locationService + "/testWebSocketContract.json"
    )
    shouldMatchFile(s"$testOutput/$locationService/models.json", locationService + "/testModels.json")
    shouldMatchFile(s"$testOutput/$locationService/README.md", locationService + "/README.md")
    shouldMatchFile(s"$testOutput/allServiceData.json", "testAllServiceData.json")
    shouldMatchFile(s"$testOutput/README.md", "README.md")
  }

  private def shouldMatchFile(actual: String, expected: String): Unit = {
    val actualFilePath   = Paths.get(actual)
    val expectedFilePath = Paths.get(getClass.getResource(s"/$expected").toURI)
    Files.readString(actualFilePath).trim should ===(Files.readString(expectedFilePath).trim)
  }
}
