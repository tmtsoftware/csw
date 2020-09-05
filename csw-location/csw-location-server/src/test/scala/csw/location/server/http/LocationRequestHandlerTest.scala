package csw.location.server.http

import java.net.URI

import akka.Done
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.commons.RandomUtils
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationRequest
import csw.location.api.messages.LocationRequest._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import msocket.http.post.{PostRouteFactory, ServerHttpCodecs}
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

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
  private val route              = new PostRouteFactory[LocationRequest]("post-endpoint", handler).make()

  override protected def beforeEach(): Unit = {
    reset(locationService, securityDirectives, accessToken, registrationResult)
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
  private val connection              = AkkaConnection(ComponentId(prefix, componentType))
  private val registration            = AkkaRegistration(connection, new URI(""), Metadata.empty)
  private val location                = registration.location(hostname).asInstanceOf[AkkaLocation]
  private val timeout                 = 5.seconds
  private val locations               = List(location)
  private val locationAdminRolePolicy = RealmRolePolicy("location-admin")
  private val accessTokenDirective    = BasicDirectives.extract(_ => accessToken)

  test("register must check if user has location admin role and delegate to locationService.register") {
    when(registrationResult.location).thenReturn(location)
    when(securityDirectives.sPost(locationAdminRolePolicy)).thenReturn(accessTokenDirective)
    when(locationService.register(registration)).thenReturn(Future.successful(registrationResult))

    Post("/post-endpoint", Register(registration).narrow) ~> route ~> check {
      verify(locationService).register(registration)
      verify(securityDirectives).sPost(locationAdminRolePolicy)
      responseAs[Location] should ===(location)
    }
  }

  test("unregisterAll must check if user has location admin role and delegate to locationService.unregisterAll") {
    when(securityDirectives.sPost(locationAdminRolePolicy)).thenReturn(accessTokenDirective)
    when(locationService.unregisterAll()).thenReturn(Future.successful(Done))

    Post("/post-endpoint", UnregisterAll.narrow) ~> route ~> check {
      verify(locationService).unregisterAll()
      verify(securityDirectives).sPost(locationAdminRolePolicy)
      responseAs[Done] should ===(Done)
    }
  }

  test("unregister must check if user has location admin role and delegate to locationService.unregister") {
    when(securityDirectives.sPost(locationAdminRolePolicy)).thenReturn(accessTokenDirective)
    when(locationService.unregister(connection)).thenReturn(Future.successful(Done))

    Post("/post-endpoint", Unregister(connection).narrow) ~> route ~> check {
      verify(locationService).unregister(connection)
      verify(securityDirectives).sPost(locationAdminRolePolicy)
      responseAs[Done] should ===(Done)
    }
  }

  test("Find must delegate to locationService.find") {
    val maybeLocation = Some(location)
    when(locationService.find(connection)).thenReturn(Future.successful(maybeLocation))

    Post("/post-endpoint", Find(connection).narrow) ~> route ~> check {
      verify(locationService).find(connection)
      responseAs[Option[AkkaLocation]] should ===(maybeLocation)
    }
  }

  test("Resolve must delegate to locationService.Resolve") {
    val maybeLocation = Some(location)
    when(locationService.resolve(connection, timeout)).thenReturn(Future.successful(maybeLocation))

    Post("/post-endpoint", Resolve(connection, timeout).narrow) ~> route ~> check {
      verify(locationService).resolve(connection, timeout)
      responseAs[Option[AkkaLocation]] should ===(maybeLocation)
    }
  }

  test("ListByComponentType must delegate to locationService.list") {
    when(locationService.list(componentType)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByComponentType(componentType).narrow) ~> route ~> check {
      verify(locationService).list(componentType)
      responseAs[List[Location]] should ===(locations)
    }
  }

  test("ListByConnectionType must delegate to locationService.list") {
    val connectionType = RandomUtils.randomFrom(ConnectionType.values)
    when(locationService.list(connectionType)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByConnectionType(connectionType).narrow) ~> route ~> check {
      verify(locationService).list(connectionType)
      responseAs[List[Location]] should ===(locations)
    }
  }

  test("ListByHostname must delegate to locationService.list") {
    when(locationService.list(hostname)).thenReturn(Future.successful(locations))

    Post("/post-endpoint", ListByHostname(hostname).narrow) ~> route ~> check {
      verify(locationService).list(hostname)
      responseAs[List[Location]] should ===(locations)
    }
  }

  test("ListByPrefix must delegate to locationService.listByPrefix") {
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
