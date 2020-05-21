package csw.location.agent

import java.net.URI

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import csw.commons.ResourceReader
import csw.location.api.models
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.Networks
import csw.prefix.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.duration.DurationInt

// DEOPSCSW-592: Create csw testkit for component writers
class MainTest extends ScalaTestFrameworkTestKit with AnyFunSuiteLike with ScalaFutures {

  implicit private val system: typed.ActorSystem[_] = typed.ActorSystem(Behaviors.empty, "test-system")
  private val locationService                       = HttpLocationServiceFactory.makeLocalClient

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
    super.afterAll()
  }

  test("Test with command line args | DEOPSCSW-592") {
    val name = "csw.test1"
    val port = 9999
    val args = Array("--prefix", name, "--command", "sleep 200", "--port", port.toString, "--no-exit")
    testWithTcp(args, name, port, Networks().hostname)
  }

  //DEOPSCSW-628: Add support for registering service as HTTP in location agent
  test("Test with command line args  with http option | DEOPSCSW-592, DEOPSCSW-628, CSW-96 ") {
    val name = "csw.test3"
    val port = 9998
    val path = "testPath"
    val args = Array("--prefix", name, "--command", "sleep 200", "--port", port.toString, "--no-exit", "--http", path)
    testWithHttp(args, name, port, path, Networks().hostname)
  }

  // CSW-86: Subsystem should be case-insensitive
  test("Test with config file | DEOPSCSW-592") {
    val name       = "CSW.test2"
    val configFile = ResourceReader.copyToTmp("/test2.conf").toFile
    val config     = ConfigFactory.parseFile(configFile)
    val port       = config.getInt("CSW.test2.port")

    val args = Array("--prefix", name, "--no-exit", configFile.getAbsolutePath)
    testWithTcp(args, name, port, Networks().hostname)
  }

  private def testWithTcp(args: Array[String], name: String, port: Int, hostname: String): Any = {
    val (process, wiring) = Main.start(args).get

    val connection       = TcpConnection(ComponentId(Prefix(name), ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).futureValue.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"tcp://$hostname:$port")
    process.destroy()
    wiring.actorRuntime.shutdown()
    eventually(locationService.list.futureValue shouldBe List.empty)
  }

  private def testWithHttp(args: Array[String], name: String, port: Int, path: String, hostname: String): Any = {
    val (process, wiring) = Main.start(args).get

    val connection       = HttpConnection(models.ComponentId(Prefix(name), ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).futureValue.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"http://$hostname:$port/$path")

    process.destroy()
    wiring.actorRuntime.shutdown()
    eventually(locationService.list.futureValue shouldBe List.empty)
  }
}
