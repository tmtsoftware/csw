package csw.clusterseed.client

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.clusterseed.internal.AdminWiring
import csw.location.commons.TestFutureExtension.RichFuture
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.util.{Failure, Success, Try}

trait HTTPLocationService extends FunSuiteLike with BeforeAndAfterAll with ScalaFutures {

  val (maybeWiring, maybeBinding) = Try {
    val adminWiring = AdminWiring.make(Some(3553))
    (adminWiring, adminWiring.locationHttpService.start().futureValue)
  } match {
    case Success((adminWiring, serverBinding)) ⇒ (Some(adminWiring), Some(serverBinding))
    case Failure(_)                            ⇒ (None, None)
  }

  override def afterAll(): Unit = {
    maybeWiring.map(_.actorRuntime.shutdown(UnknownReason).await)
    super.afterAll()
  }
}
