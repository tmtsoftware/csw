package csw.location.impl.http

import akka.http.scaladsl.Http
import csw.location.helpers.LSNodeSpec
import csw.location.impl.internal.ServerWiring
import csw.location.impl.commons.TestFutureExtension.RichFuture
import org.scalatest.BeforeAndAfterAll

import scala.util.Try

trait MultiNodeHTTPLocationService {
  self: LSNodeSpec[_] with BeforeAndAfterAll =>
  private val maybeBinding: Option[Http.ServerBinding] = Try {
    val binding = ServerWiring.make(self.typedSystem, "csw-location-server").locationHttpService.start()
    Some(binding.await)
  } match {
    case _ => None // ignore binding errors
  }

  override def afterAll(): Unit = {
    maybeBinding.foreach(_.unbind().await)
    multiNodeSpecAfterAll()
  }

}
