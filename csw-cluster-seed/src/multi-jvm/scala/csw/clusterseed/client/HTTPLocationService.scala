package csw.clusterseed.client

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.clusterseed.internal.AdminWiring
import csw.event.helpers.TestFutureExt.RichFuture
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.util.{Failure, Success, Try}

trait HTTPLocationService extends FunSuiteLike with BeforeAndAfterAll {

  val (maybeWiring, maybeBinding) = Try {
    val adminWiring = AdminWiring.make(Some(3553), None)
    (adminWiring, adminWiring.locationHttpService.start().await)
  } match {
    case Success((adminWiring, serverBinding)) ⇒ (Some(adminWiring), Some(serverBinding))
    case Failure(_)                            ⇒ (None, None)
  }

  override def afterAll(): Unit = {
    maybeWiring.map(_.actorRuntime.shutdown(UnknownReason).await)
    super.afterAll()
  }
}
