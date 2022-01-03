package csw.aas.http

import java.net.URI
import akka.http.scaladsl.model.HttpMethods.*
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigValueFactory
import csw.aas.core.commons.AASConnection
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{HttpLocation, Metadata}
import csw.location.api.scaladsl.LocationService
import msocket.security.api.AuthorizationPolicy
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class SecurityDirectivesTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("sGet should call validate with HttpMethod GET and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sGet(policy)

    verify(policyValidator).validate(GET, policy)
  }

  test("sPost should call validate with HttpMethod POST and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sPost(policy)

    verify(policyValidator).validate(POST, policy)
  }

  test("sDelete should call validate with HttpMethod DELETE and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sDelete(policy)

    verify(policyValidator).validate(DELETE, policy)
  }

  test("sHead should call validate with HttpMethod HEAD and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sHead(policy)

    verify(policyValidator).validate(HEAD, policy)
  }

  test("sPatch should call validate with HttpMethod PATCH and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sPatch(policy)

    verify(policyValidator).validate(PATCH, policy)
  }

  test("sPut should call validate with HttpMethod PUT and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sPut(policy)

    verify(policyValidator).validate(PUT, policy)
  }

  test("sConnect should call validate with HttpMethod CONNECT and policy | DEOPSCSW-579") {
    val policyValidator    = mock[PolicyValidator]
    val policy             = mock[AuthorizationPolicy]
    val securityDirectives = new SecurityDirectives(policyValidator)

    securityDirectives.sConnect(policy)

    verify(policyValidator).validate(CONNECT, policy)
  }

  test("apply should not resolve AAS location when auth param is disabled | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    SecurityDirectives(system.settings.config, locationService, enableAuth = false)
    verify(locationService, Mockito.never()).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("apply should resolve AAS location when auth param is enabled | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    when(locationService.resolve(any[HttpConnection], any[FiniteDuration]))
      .thenReturn(Future.successful(Some(HttpLocation(AASConnection.value, URI.create(""), Metadata.empty))))
    SecurityDirectives(system.settings.config, locationService, enableAuth = true)
    verify(locationService).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("authDisabled should never resolve AAS location | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    SecurityDirectives.authDisabled(system.settings.config)
    verify(locationService, Mockito.never()).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("apply should not resolve AAS location when auth is disabled in config | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    val config                           = system.settings.config.withValue("auth-config.disabled", ConfigValueFactory.fromAnyRef("true"))
    SecurityDirectives(config, locationService)
    verify(locationService, Mockito.never()).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("apply should resolve AAS location when auth is enabled in config | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    val config                           = system.settings.config.withValue("auth-config.disabled", ConfigValueFactory.fromAnyRef("false"))
    when(locationService.resolve(any[HttpConnection], any[FiniteDuration]))
      .thenReturn(Future.successful(Some(HttpLocation(AASConnection.value, URI.create(""), Metadata.empty))))
    SecurityDirectives(config, locationService)
    verify(locationService).resolve(any[HttpConnection], any[FiniteDuration])
  }
}
