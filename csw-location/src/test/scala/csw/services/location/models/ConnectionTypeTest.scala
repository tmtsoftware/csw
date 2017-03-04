package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType, UnknownConnectionTypeException}
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ConnectionTypeTest extends FunSuite with Matchers {

  test("should successfully parse connection type string representations") {
    ConnectionType.parse("http").shouldEqual(Success(HttpType))
    ConnectionType.parse("akka").shouldEqual(Success(AkkaType))
    ConnectionType.parse("tcp").shouldEqual(Success(TcpType))
  }

  test("should fail to parse invalid connection type string") {
    val parsedConnectionType = ConnectionType.parse("ftp")
    parsedConnectionType.shouldEqual(Failure(UnknownConnectionTypeException("ftp")))
  }
}
