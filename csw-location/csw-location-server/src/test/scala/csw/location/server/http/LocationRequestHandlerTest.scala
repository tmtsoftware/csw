/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.server.directives.BasicDirectives
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.commons.RandomUtils
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationRequest
import csw.location.api.messages.LocationRequest.*
import csw.location.api.models.*
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import msocket.http.post.{PostRouteFactory, ServerHttpCodecs}
import msocket.jvm.metrics.LabelExtractor
import msocket.security.models.AccessToken
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock

import java.net.URI
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class LocationRequestHandlerTest
    extends AnyFunSuite
    with ScalatestRouteTest
    with Matchers
    with LocationServiceCodecs
    with ServerHttpCodecs
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val accessToken        = mock[AccessToken]
  private val registrationResult = mock[RegistrationResult]
  private val locationService    = mock[LocationService]
  private val securityDirectives = mock[SecurityDirectives]
  private val handler            = new LocationRequestHandler(locationService, securityDirectives)

  import LabelExtractor.Implicits.default
  private val route = new PostRouteFactory[LocationRequest]("post-endpoint", handler).make()

  override protected def beforeEach(): Unit = {
    reset(locationService)
    reset(securityDirectives)
    reset(accessToken)
    reset(registrationResult)
    super.beforeEach()
  }

  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
    super.afterAll()
  }

  private val subsystem               = randomSubsystem
  private val componentType           = randomComponentType
  private val hostname                = RandomUtils.randomString5()
  private val prefix                  = Prefix(subsystem, RandomUtils.randomString5())
  private val connection              = PekkoConnection(ComponentId(prefix, componentType))
  private val registration            = PekkoRegistration(connection, new URI(""), Metadata.empty)
  private val location                = registration.location(hostname).asInstanceOf[PekkoLocation]
  private val timeout                 = 5.seconds
  private val locations               = List(location)
  private val locationAdminRolePolicy = RealmRolePolicy("location-admin")
  private val accessTokenDirective    = BasicDirectives.extract(_ => accessToken)

  test("register must check if user has location admin role and delegate to locationService.register | CSW-98") {
    when(registrationResult.location).thenReturn(location)
    when(securityDirectives.sPost(locationAdminRolePolicy)).thenReturn(accessTokenDirective)
    when(locationService.register(registration)).thenReturn(Future.successful(registrationResult))

    Post("/post-endpoint", Register(registration).narrow) ~> route ~> check {
      verify(locationService).register(registration)
      verify(securityDirectives).sPost(locationAdminRolePolicy)
      responseAs[Location] should ===(location)
    }
  }

  test("unregisterAll must check if user has location admin role and delegate to locationService.unregisterAll | CSW-98 ") {
    when(securityDirectives.sPost(locationAdminRolePolicy)).thenReturn(accessTokenDirective)
    when(locationService.unregisterAll()).thenReturn(Future.successful(Done))

    Post("/post-endpoint", UnregisterAll.narrow) ~> route ~> check {
      verify(locationService).unregisterAll()
      verify(securityDirectives).sPost(locationAdminRolePolicy)
      responseAs[Done] should ===(Done)
    }
  }

  test("unregister must check if user has location admin role and delegate to locationService.unregister | CSW-98") {
    when(securityDirectives.sPost(locationAdminRolePolicy)).thenReturn(accessTokenDirective)
    when(locationService.unregister(connection)).thenReturn(Future.successful(Done))

    Post("/post-endpoint", Unregister(connection).narrow) ~> route ~> check {
      verify(locationService).unregister(connection)
      verify(securityDirectives).sPost(locationAdminRolePolicy)
      responseAs[Done] should ===(Done)
    }
  }

  test("Find must delegate to locationService.find | CSW-98") {
    val maybeLocation = Some(location)
    when(locationService.find(connection)).thenReturn(Future.successful(maybeLocation))

    Post("/post-endpoint", Find(connection).narrow) ~> route ~> check {
      verify(locationService).find(connection)
      responseAs[Option[PekkoLocation]] should ===(maybeLocation)
    }
  }

  test("Resolve must delegate to locationService.Resolve | CSW-98") {
    val maybeLocation = Some(location)
    when(locationService.resolve(connection, timeout)).thenReturn(Future.successful(maybeLocation))

    Post("/post-endpoint", Resolve(connection, timeout).narrow) ~> route ~> check {
      verify(locationService).resolve(connection, timeout)
      responseAs[Option[PekkoLocation]] should ===(maybeLocation)
    }
  }

  test("ListByComponentType must delegate to locationService.list | CSW-98") {
    when(locationService.list(componentType)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByComponentType(componentType).narrow) ~> route ~> check {
      verify(locationService).list(componentType)
      responseAs[List[Location]] should ===(locations)
    }
  }

  test("ListByConnectionType must delegate to locationService.list | CSW-98") {
    val connectionType = RandomUtils.randomFrom(ConnectionType.values)
    when(locationService.list(connectionType)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByConnectionType(connectionType).narrow) ~> route ~> check {
      verify(locationService).list(connectionType)
      responseAs[List[Location]] should ===(locations)
    }
  }

  test("ListByHostname must delegate to locationService.list | CSW-98") {
    when(locationService.list(hostname)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByHostname(hostname).narrow) ~> route ~> check {
      verify(locationService).list(hostname)
      responseAs[List[Location]] should ===(locations)
    }
  }

  test("ListByPrefix must delegate to locationService.listByPrefix | CSW-98") {
    val prefixStr = prefix.toString
    when(locationService.listByPrefix(prefixStr)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByPrefix(prefixStr).narrow) ~> route ~> check {
      verify(locationService).listByPrefix(prefixStr)
      responseAs[List[Location]] should ===(locations)
    }
  }

  private def randomSubsystem: Subsystem         = RandomUtils.randomFrom(Subsystem.values)
  private def randomComponentType: ComponentType = RandomUtils.randomFrom(ComponentType.values)

  private implicit class Narrower(x: LocationRequest) {
    def narrow: LocationRequest = x
  }
}
