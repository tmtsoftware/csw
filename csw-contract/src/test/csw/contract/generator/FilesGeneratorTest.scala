package csw.contract.generator
import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}

import csw.contract.ResourceFetcher
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationHttpMessage.Register
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models._
import csw.prefix.models.Prefix
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.reflect.io.Directory

class FilesGeneratorTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with LocationServiceCodecs {

  override def afterAll(): Unit = {
    val dir = new Directory(new File("csw-contract/src/test/testOutput"))
    if (dir.exists) {
      dir.deleteRecursively()
    }
  }

  test("should generate samples for given services") {
    val componentId: ComponentId           = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
    val akkaConnection: AkkaConnection     = AkkaConnection(componentId)
    val httpConnection: HttpConnection     = HttpConnection(componentId)
    val akkaRegistration: AkkaRegistration = AkkaRegistration(akkaConnection, new URI("somePath"))
    val httpRegistration: HttpRegistration = HttpRegistration(httpConnection, 2090, "somePath")
    val akkaLocation: Location             = AkkaLocation(akkaConnection, new URI("some_path"))
    val akkaRegister: Register             = Register(akkaRegistration)
    val httpRegister: Register             = Register(httpRegistration)
    val track: Track                       = Track(akkaConnection)
    val httpEndpoints: List[Endpoint] = List(
      Endpoint(
        requestType = "Register",
        responseType = "RegistrationFailed",
        errorTypes = Nil
      )
    )
    val websocketEndpoints: List[Endpoint] = List(
      Endpoint(
        requestType = "Track",
        responseType = "TrackingEvent",
        errorTypes = List("ServiceError")
      )
    )
    val models: ModelSet = ModelSet(
      ModelType(akkaLocation)
    )
    val httpRequests = ModelSet(
      ModelType[LocationHttpMessage](akkaRegister, httpRegister)
    )
    val webSocketRequests = ModelSet(
      ModelType[LocationWebsocketMessage](track)
    )
    val services: Services = Services(
      Map(
        "location-service" -> Service(
          `http-contract` = Contract(httpEndpoints, httpRequests),
          `websocket-contract` = Contract(websocketEndpoints, webSocketRequests),
          models,
          Readme(ResourceFetcher.getResourceAsString("location-service/README.md"))
        )
      )
    )
    val testOutput = "csw-contract/src/test/testOutput"
    FilesGenerator.generate(services, testOutput)
    val path = Paths.get(testOutput)
    Files.exists(path) shouldBe true
    Files.size(path) should be > 0L
  }
}
