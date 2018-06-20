package csw.apps.clusterseed.client

import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.messages.commons.CoordinatedShutdownReasons
import csw.services.location.TrackLocationTest

import scala.util.{Failure, Success, Try}

class TrackLocationTestWithHttpMultiJvmNode1 extends TrackLocationTestWithHttp(0)
class TrackLocationTestWithHttpMultiJvmNode2 extends TrackLocationTestWithHttp(0)
class TrackLocationTestWithHttpMultiJvmNode3 extends TrackLocationTestWithHttp(0)

class TrackLocationTestWithHttp(ignore: Int) extends TrackLocationTest(0, "http") {

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
