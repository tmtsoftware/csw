package example.teskit

import com.typesafe.config.ConfigFactory
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.testkit.FrameworkTestKit
import csw.testkit.scaladsl.CSWService.{ConfigServer, EventServer}
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers, OptionValues}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class TestKitsExampleTest extends FunSuiteLike with BeforeAndAfterAll with Matchers with OptionValues {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  //#framework-testkit
  // create instance of framework testkit
  private val frameworkTestKit = FrameworkTestKit()

  // starts Config Server and Event Service
  override protected def beforeAll(): Unit = frameworkTestKit.start(ConfigServer, EventServer)

  // stops all services started by this testkit
  override protected def afterAll(): Unit = frameworkTestKit.shutdown()
  //#framework-testkit

  import frameworkTestKit.frameworkWiring.actorRuntime._
  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  test("framework testkit example for spawning container") {
    //#spawn-using-testkit

    // starting container from container config using testkit
    frameworkTestKit.spawnContainer(ConfigFactory.load("SampleContainer.conf"))

    // starting standalone component from config using testkit
    // val componentRef: ActorRef[ComponentMessage] =
    //   frameworkTestKit.spawnStandaloneComponent(ConfigFactory.load("SampleHcdStandalone.conf"))

    //#spawn-using-testkit

    val connection       = AkkaConnection(ComponentId("SampleAssembly", Assembly))
    val assemblyLocation = Await.result(locationService.resolve(connection, 5.seconds), 10.seconds)
    assemblyLocation.value.connection shouldBe connection
  }

}
