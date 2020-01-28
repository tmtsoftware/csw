package csw.location.server.http

import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

trait HTTPLocationService
    extends AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with MockitoSugar {

  private val locationPort                         = 3553
  private var locationWiring: Option[ServerWiring] = None

  def start(clusterPort: Option[Int] = Some(locationPort), httpPort: Option[Int] = None): Unit = {
    locationWiring = Some(ServerWiring.make(clusterPort, httpPort))
    locationWiring.map(_.locationHttpService.start().await)
  }

  override def beforeAll(): Unit = start()
  override def afterAll(): Unit  = locationWiring.map(_.actorRuntime.shutdown().await)

}

class JHTTPLocationService extends HTTPLocationService

class HTTPLocationServiceOnPorts(clusterPort: Int, httpPort: Int) extends HTTPLocationService {
  override def beforeAll(): Unit = start(Some(clusterPort), Some(httpPort))
}
