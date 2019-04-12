package csw.location.server.http

import akka.http.scaladsl.Http
import csw.location.helpers.LSNodeSpec
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future
import scala.util.Try

trait MultiNodeHTTPLocationService {
  self: LSNodeSpec[_] with BeforeAndAfterAll =>
  private var eventualBinding: Future[Http.ServerBinding] = _

  Try {
    eventualBinding = ServerWiring.make(self.system).locationHttpService.start()
    eventualBinding.await
  } match {
    case _ => // ignore binding errors
  }

  override def afterAll(): Unit = {
    import self.system.dispatcher
    eventualBinding.flatMap(_.unbind()).await
    multiNodeSpecAfterAll()
  }

}
