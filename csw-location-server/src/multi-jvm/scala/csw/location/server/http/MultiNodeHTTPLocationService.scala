package csw.location.server.http

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import csw.location.helpers.LSNodeSpec
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import org.scalatest.BeforeAndAfterAll

import scala.util.Try

trait MultiNodeHTTPLocationService {
  self: LSNodeSpec[_] with BeforeAndAfterAll =>

  Try(ServerWiring.make(self.system).locationHttpService.start().await) match {
    case _ => // ignore binding errors
  }

  override def afterAll(): Unit = {
    CoordinatedShutdown(self.system).run(UnknownReason).await
    multiNodeSpecBeforeAll()
  }

}
