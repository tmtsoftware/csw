package csw.apps.clusterseed.client

import csw.apps.clusterseed.internal.AdminWiring
import csw.messages.commons.CoordinatedShutdownReasons
import csw.services.event.helpers.TestFutureExt.RichFuture
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.util.{Failure, Success, Try}

trait HTTPLocationService extends FunSuiteLike with BeforeAndAfterAll {

  val (wiring, binding) = Try {
    val adminWiring = AdminWiring.make(Some(3553), None)
    (adminWiring, adminWiring.locationHttpService.start().await)
  } match {
    case Success((adminWiring, serverBinding)) ⇒ (Some(adminWiring), Some(serverBinding))
    case Failure(_)                            ⇒ (None, None)
  }

  override def afterAll(): Unit = {
    binding.map(_.unbind().await)
    wiring.map(_.locationService.shutdown(CoordinatedShutdownReasons.testFinishedReason).await)
    super.afterAll()
  }
}
