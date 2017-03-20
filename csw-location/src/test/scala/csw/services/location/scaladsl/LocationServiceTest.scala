package csw.services.location.scaladsl

import csw.services.location.common.TestFutureExtension.RichFuture
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class LocationServiceTest
  extends FunSuite
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll {

  val actorRuntime = new ActorRuntime("test")

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }
  
  test("future should contain a exception in case jmDNS throws an error") {
  }

  case object CustomExp extends RuntimeException

}
