package csw.apps.clusterseed.client

import akka.http.scaladsl.Http
import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.LocationServiceCompTest

class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {

  private val wiring = new AdminWiring

  val binding: Http.ServerBinding = wiring.locationHttpService.start().await

  override protected def afterAll(): Unit = {
    binding.unbind().await
    super.afterAll()
  }
}
