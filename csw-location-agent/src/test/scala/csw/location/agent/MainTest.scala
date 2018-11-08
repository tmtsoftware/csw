package csw.location.agent

import java.net.URI
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.location.agent.common.TestFutureExtension.RichFuture
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.Networks
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.FunSuiteLike

import scala.concurrent.duration._

// DEOPSCSW-592: Create csw testkit for component writers
class MainTest extends ScalaTestFrameworkTestKit with FunSuiteLike {

  implicit private val system: ActorSystem    = ActorSystem()
  implicit private val mat: ActorMaterializer = ActorMaterializer()
  private val locationService                 = HttpLocationServiceFactory.makeLocalClient

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override def afterAll(): Unit = {
    system.terminate().await
    super.afterAll()
  }

  test("Test with command line args") {
    val name = "test1"
    val port = 9999
    val args = Array("--name", name, "--command", "sleep 200", "--port", port.toString, "--no-exit")
    testWith(args, name, port)
  }

  test("Test with config file") {
    val name       = "test2"
    val url        = getClass.getResource("/test2.conf")
    val configFile = Paths.get(url.toURI).toFile
    val config     = ConfigFactory.parseFile(configFile)
    val port       = config.getInt("test2.port")

    val args = Array("--name", name, "--no-exit", configFile.getAbsolutePath)
    testWith(args, name, port)
  }

  private def testWith(args: Array[String], name: String, port: Int) = {
    val process = Main.start(args).get

    val connection       = TcpConnection(ComponentId(name, ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"tcp://${Networks().hostname}:$port")

    process.destroy()
    eventually(locationService.list.await shouldBe List.empty)
  }
}
