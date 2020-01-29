package csw.config.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import com.typesafe.config.ConfigFactory
import csw.aas.core.commons.AASConnection
import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.commons.{ConfigServiceConnection, TestFileUtils}
import csw.location.api.models
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import org.tmatesoft.svn.core.SVNException

import scala.concurrent.duration._

// DEOPSCSW-130: Command line App for HTTP server
class MainTest extends HTTPLocationService {
  implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "config-server")

  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))
  private val AASPort       = 8080

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
    //register AAS with location service
    locationService.register(models.HttpRegistration(AASConnection.value, AASPort, "auth"))
  }

  override def afterEach(): Unit = testFileUtils.deleteServerFiles()

  override def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.await
    super.afterAll()
  }

  test("should not start HTTP server if repo does not exist") {
    val actualException = intercept[Exception] {
      Main.start(Array.empty).get
    }

    actualException.getCause shouldBe a[SVNException]

    locationService.find(ConfigServiceConnection.value).await shouldBe None
  }

  test("should init svn repo and register with location service if --initRepo option is provided") {
    val httpService = Main.start(Array("--initRepo")).get

    try {
      val configServiceLocation = locationService.resolve(ConfigServiceConnection.value, 5.seconds).await.get
      configServiceLocation.connection shouldBe ConfigServiceConnection.value

      val uri = Uri(configServiceLocation.uri.toString).withPath(Path / "list")

      val request  = HttpRequest(uri = uri)
      val response = Http()(actorSystem.toClassic).singleRequest(request).await
      response.status shouldBe StatusCodes.OK
      response.discardEntityBytes()
    }
    finally {
      httpService.shutdown().await
    }
  }

  test("should not initialize svn repo if --initRepo option is not provided and should use existing repo if available") {

    // temporary start a server to create a repo and then shutdown the server
    val tmpHttpService = Main.start(Array("--initRepo")).get
    tmpHttpService.shutdown().await

    val httpService = Main.start(Array.empty).get

    try {
      val configServiceLocation = locationService.resolve(ConfigServiceConnection.value, 5.seconds).await.get

      configServiceLocation.connection shouldBe ConfigServiceConnection.value
      val uri = Uri(configServiceLocation.uri.toString).withPath(Path / "list")

      val request  = HttpRequest(uri = uri)
      val response = Http()(actorSystem.toClassic).singleRequest(request).await
      response.status shouldBe StatusCodes.OK
    }
    finally {
      httpService.shutdown().await
    }
  }
}
