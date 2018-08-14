package csw.apps.clusterseed.client

import akka.actor.ActorSystem
import csw.apps.clusterseed.internal.AdminWiring
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.LoggingSystemFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

trait HTTPLocationService extends FunSuiteLike with BeforeAndAfterAll {

  val testSystem: ActorSystem               = ActorSystemFactory.remote()
  private implicit val ec: ExecutionContext = testSystem.dispatcher

  LoggingSystemFactory.start("multi-jvm-http", "master", "localhost", testSystem)

  val (maybeWiring, maybeBinding) = Try {
    val adminWiring = AdminWiring.make(Some(3553), None)
    (adminWiring, adminWiring.locationHttpService.start().await)
  } match {
    case Success((adminWiring, serverBinding)) ⇒ (Some(adminWiring), Some(serverBinding))
    case Failure(_)                            ⇒ (None, None)
  }

  override def afterAll(): Unit = {
    maybeBinding.map(_.terminate(5.seconds).await)
    super.afterAll()
    maybeWiring.map(_.actorRuntime.shutdown(TestFinishedReason).await)
    testSystem.terminate().await
  }
}
