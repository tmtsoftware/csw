package csw.contract.generator
import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}

import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.{Endpoint, Service, Services}
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions.{LocationServiceError, RegistrationFailed}
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage.Register
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType, Registration}
import csw.prefix.models.Prefix
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.reflect.io.Directory

class FilesGeneratorTest extends FunSuite with Matchers with BeforeAndAfterAll with LocationServiceCodecs {

  override def afterAll(): Unit = {
    val dir = new Directory(new File("testOutput"))
    if (dir.exists) {
      dir.deleteRecursively()
    }
  }
  test("should generate samples for given services") {
    val componentId: ComponentId                 = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
    val akkaConnection: AkkaConnection           = AkkaConnection(componentId)
    val akkaRegistration: Registration           = AkkaRegistration(akkaConnection, new URI("some_path"))
    val registerAkka: LocationHttpMessage        = Register(akkaRegistration)
    val registrationFailed: LocationServiceError = RegistrationFailed("message")
    val endpoints: Map[String, Endpoint] = Map(
      "register" -> Endpoint(
        requests = List(registerAkka),
        responses = List(registrationFailed)
      )
    )
    val services: Services = Services(
      Map(
        "location" -> Service(
          endpoints,
          Map.empty
        )
      )
    )
    val testOutput = "testOutput"
    FilesGenerator.generate(services, testOutput)
    val path = Paths.get(testOutput)
    Files.exists(path) shouldBe true
    Files.size(path) should be > 0L
  }
}
