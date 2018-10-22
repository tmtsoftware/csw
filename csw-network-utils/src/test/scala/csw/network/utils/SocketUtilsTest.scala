package csw.network.utils
import java.net.ServerSocket

import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class SocketUtilsTest extends FunSuite with BeforeAndAfterAll with Matchers {
  val serverPort   = 7654
  val serverSocket = new ServerSocket(serverPort)

  override protected def afterAll(): Unit = serverSocket.close()

  test("should return true when server is running on given host and port") {
    SocketUtils.serverUp("localhost", 7654) shouldBe true
  }

  test("should throw exception when server is not running on given host and port") {
    val msg = "Not running"
    val ex  = intercept[IllegalArgumentException](SocketUtils.requireServerUp("invalid-ip", 7654, msg = msg))
    ex.getMessage shouldBe "requirement failed: " + msg
  }

}
