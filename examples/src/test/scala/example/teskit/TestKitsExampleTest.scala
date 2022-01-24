package example.teskit

import com.typesafe.config.ConfigFactory
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.FrameworkTestKit
import csw.testkit.scaladsl.CSWService.{ConfigServer, EventServer}
import io.netty.util.internal.logging.InternalLoggerFactory
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.tmt.csw.sample.SampleHandlers
import org.tmt.csw.samplehcd.SampleHcdHandlers

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class TestKitsExampleTest extends AnyFunSuiteLike with BeforeAndAfterAll with Matchers with OptionValues {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(InternalLoggerFactory.getDefaultFactory)

  // #framework-testkit
  // create instance of framework testkit
  private val frameworkTestKit = FrameworkTestKit()

  // starts Config Server and Event Service
  override protected def beforeAll(): Unit = frameworkTestKit.start(ConfigServer, EventServer)

  // stops all services started by this testkit
  override protected def afterAll(): Unit = frameworkTestKit.shutdown()
  // #framework-testkit

  import frameworkTestKit._

  test("framework testkit example for spawning container") {
    // #spawn-using-testkit

    // starting container from container config using testkit
    frameworkTestKit.spawnContainer(ConfigFactory.load("SampleContainer.conf"))

    // starting standalone component from config using testkit
    // val componentRef: ActorRef[ComponentMessage] =
    //   frameworkTestKit.spawnStandaloneComponent(ConfigFactory.load("SampleHcdStandalone.conf"))

    // #spawn-using-testkit

    val connection       = AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "sample"), Assembly))
    val assemblyLocation = Await.result(locationService.resolve(connection, 5.seconds), 10.seconds)
    assemblyLocation.value.connection shouldBe connection
  }

  test("framework testkit example for spawning hcd and assembly without config") {

    // #spawn-assembly
    frameworkTestKit.spawnAssembly(Prefix("TCS.sampleAssembly"), (ctx, cswCtx) => new SampleHandlers(ctx, cswCtx))
    // #spawn-assembly

    val assemblyConnection = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "sampleAssembly"), Assembly))
    val assemblyLocation   = Await.result(locationService.resolve(assemblyConnection, 5.seconds), 10.seconds)
    assemblyLocation.value.connection shouldBe assemblyConnection

    // #spawn-hcd
    frameworkTestKit.spawnHCD(Prefix("TCS.sampleHcd"), (ctx, cswCtx) => new SampleHcdHandlers(ctx, cswCtx))
    // #spawn-hcd

    val hcdConnection = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "sampleHcd"), HCD))
    val hcdLocation   = Await.result(locationService.resolve(hcdConnection, 5.seconds), 10.seconds)
    hcdLocation.value.connection shouldBe hcdConnection
  }

}
