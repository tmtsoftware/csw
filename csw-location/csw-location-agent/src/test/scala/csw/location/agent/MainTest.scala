package csw.location.agent

import java.net.URI

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import csw.commons.ResourceReader
import csw.location.agent.common.TestFutureExtension.RichFuture
import csw.location.api.models
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType, NetworkType}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.Networks
import csw.prefix.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.duration._

// DEOPSCSW-592: Create csw testkit for component writers
class MainTest extends ScalaTestFrameworkTestKit with AnyFunSuiteLike {

  implicit private val system: typed.ActorSystem[_] = typed.ActorSystem(Behaviors.empty, "test-system")
  private val locationService                       = HttpLocationServiceFactory.makeLocalClient

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.await
    super.afterAll()
  }

  test("Test with command line args | DEOPSCSW-592") {
    val name = "csw.test1"
    val port = 9999
    val args = Array("--prefix", name, "--command", "sleep 200", "--port", port.toString, "--no-exit")
    testWithTcp(args, name, port)
  }

  //DEOPSCSW-628: Add support for registering service as HTTP in location agent
  test(
    "Test with command line args with http option and default private Network option | DEOPSCSW-592, DEOPSCSW-628 | " +
      "CSW-96"
  ) {
    val name = "csw.test3"
    val port = 9998
    val path = "testPath"
    val args = Array("--prefix", name, "--command", "sleep 200", "--port", port.toString, "--no-exit", "--http", path)
    testWithHttp(args, name, port, path, Networks().hostname)
  }

  test(
    "Test with command line args with http and public Network option | DEOPSCSW-592, DEOPSCSW-628 | " +
      "CSW-96"
  ) {
    val name = "csw.test3"
    val port = 9998
    val path = "testPath"
    val args =
      Array("--prefix", name, "--command", "sleep 200", "--port", port.toString, "--no-exit", "--http", path, "--publicNetwork")
    testWithHttp(args, name, port, path, Networks(NetworkType.Public.envKey).hostname)
  }

  // CSW-86: Subsystem should be case-insensitive
  test("Test with config file | DEOPSCSW-592") {
    val name       = "CSW.test2"
    val configFile = ResourceReader.copyToTmp("/test2.conf").toFile
    val config     = ConfigFactory.parseFile(configFile)
    val port       = config.getInt("CSW.test2.port")

    val args = Array("--prefix", name, "--no-exit", configFile.getAbsolutePath)
    testWithTcp(args, name, port)
  }

  private def testWithTcp(args: Array[String], name: String, port: Int) = {
    val process = Main.start(args).get._1

    val connection       = TcpConnection(ComponentId(Prefix(name), ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"tcp://${Networks().hostname}:$port")

    process.destroy()
    eventually(locationService.list.await shouldBe List.empty)
  }

  private def testWithHttp(args: Array[String], name: String, port: Int, path: String, hostname: String): Any = {
    val process = Main.start(args).get._1

    val connection       = HttpConnection(models.ComponentId(Prefix(name), ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"http://${hostname}:$port/$path")

    process.destroy()
    eventually(locationService.list.await shouldBe List.empty)
  }
}
