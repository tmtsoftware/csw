package csw.apps.clusterseed.client

import akka.http.scaladsl.Http
import csw.apps.clusterseed.internal.AdminWiring
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.LoggingSystemFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.util.{Failure, Success, Try}

trait HTTPLocationService extends FunSuiteLike with BeforeAndAfterAll {

  private val testSystem = ActorSystemFactory.remote()
  LoggingSystemFactory.start("multi-jvm-http", "master", "localhost", testSystem)

  val (maybeWiring, maybeBinding) = Try {
    val adminWiring = AdminWiring.make(Some(3553), None)
    (adminWiring, adminWiring.locationHttpService.start().await)
  } match {
    case Success((adminWiring, serverBinding)) ⇒ (Some(adminWiring), Some(serverBinding))
    case Failure(_)                            ⇒ (None, None)
  }

  override def afterAll(): Unit = {
    maybeBinding.map(_.unbind().await)
    maybeWiring.map { wiring ⇒
      Http(wiring.actorSystem).shutdownAllConnectionPools().await
      wiring.actorRuntime.shutdown(TestFinishedReason).await
    }
    super.afterAll()
    testSystem.terminate().await
  }
}
