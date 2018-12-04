//package csw.aas.core.deployment
//import akka.http.scaladsl.model.Uri
//import csw.location.api.scaladsl.LocationService
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class LocationServiceIntegration(locationService: LocationService)(implicit executionContext: ExecutionContext) {
//  val keycloakServiceResolver = new KeycloakServiceResolver(locationService)
//
//  def resolveKeycloak: Future[Uri] = {
//    keycloakServiceResolver.resolve
//  }
//}
