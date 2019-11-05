package csw.location.agent

import java.net.URI

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import csw.commons.ResourceReader
import csw.location.agent.common.TestFutureExtension.RichFuture
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.{HttpConnection, TcpConnection}
import csw.location.models.{ComponentId, ComponentType}
import csw.network.utils.Networks
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.FunSuiteLike

import scala.concurrent.duration._

// DEOPSCSW-592: Create csw testkit for component writers
class MainTest extends ScalaTestFrameworkTestKit with FunSuiteLike {

  implicit private val system: typed.ActorSystem[_] = typed.ActorSystem(Behaviors.empty, "test-system")
  implicit private val mat: Materializer            = Materializer(system)
  private val locationService                       = HttpLocationServiceFactory.makeLocalClient

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.await
    super.afterAll()
  }

  test("Test with command line args") {
    val name = "test1"
    val port = 9999
    val args = Array("--name", name, "--command", "sleep 200", "--port", port.toString, "--no-exit")
    testWithTcp(args, name, port)
  }

  //DEOPSCSW-628: Add support for registering service as HTTP in location agent
  test("Test with command line args with http option") {
    val name = "test3"
    val port = 9998
    val path = "testPath"
    val args = Array("--name", name, "--command", "sleep 200", "--port", port.toString, "--no-exit", "--http", path)
    testWithHttp(args, name, port, path)
  }

  test("Test with config file") {
    val name       = "test2"
    val configFile = ResourceReader.copyToTmp("/test2.conf").toFile
    val config     = ConfigFactory.parseFile(configFile)
    val port       = config.getInt("test2.port")

    val args = Array("--name", name, "--no-exit", configFile.getAbsolutePath)
    testWithTcp(args, name, port)
  }

  private def testWithTcp(args: Array[String], name: String, port: Int) = {
    val process = Main.start(args).get

    val connection       = TcpConnection(ComponentId(name, ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"tcp://${Networks().hostname}:$port")

    process.destroy()
    eventually(locationService.list.await shouldBe List.empty)
  }

  private def testWithHttp(args: Array[String], name: String, port: Int, path: String) = {
    val process = Main.start(args).get

    val connection       = HttpConnection(ComponentId(name, ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get

    resolvedLocation.connection shouldBe connection
    resolvedLocation.uri shouldBe new URI(s"http://${Networks().hostname}:$port/$path")

    process.destroy()
    eventually(locationService.list.await shouldBe List.empty)
  }
}
