/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.network.utils
import java.net.ServerSocket

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SocketUtilsTest extends AnyFunSuite with BeforeAndAfterAll with Matchers {
  val serverPort   = 7654
  val serverSocket = new ServerSocket(serverPort)

  override protected def afterAll(): Unit = serverSocket.close()

  test("should return true when server is running on given host and port") {
    SocketUtils.isAddressInUse("localhost", 7654) shouldBe true
  }

  test("should throw exception when server is not running on given host and port") {
    val msg = "Not running"
    val ex  = intercept[IllegalArgumentException](SocketUtils.requireServerUp("invalid-ip", 7654, msg = msg))
    ex.getMessage shouldBe "requirement failed: " + msg
  }

}
