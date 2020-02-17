package csw.location.server.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

trait HTTPLocationService
    extends AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with MockitoSugar {

  protected val locationPort: Int             = 3553
  protected val httpLocationPort: Option[Int] = None
  var locationWiring: Option[ServerWiring]    = None
  protected val enableAuth: Boolean           = false

  def start(clusterPort: Option[Int] = Some(locationPort), httpPort: Option[Int] = httpLocationPort): Unit = {
    locationWiring = Some(ServerWiring.make(clusterPort, httpPort, enableAuth = enableAuth))
    locationWiring.map(_.locationHttpService.start().await)
  }

  override def beforeAll(): Unit = start()
  override def afterAll(): Unit  = locationWiring.map(_.actorRuntime.shutdown().await)

}

class JHTTPLocationService extends HTTPLocationService

class HTTPLocationServiceOnPorts(clusterPort: Int, val httpPort: Int, auth: Boolean = false) extends HTTPLocationService {
  override val locationPort: Int             = clusterPort
  override val enableAuth: Boolean           = auth
  override val httpLocationPort: Option[Int] = Some(httpPort)
}
