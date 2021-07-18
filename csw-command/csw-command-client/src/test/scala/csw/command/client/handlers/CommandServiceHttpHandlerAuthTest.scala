package csw.command.client.handlers

import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.SecurityDirectives
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceRequest
import csw.command.api.messages.CommandServiceRequest.{Oneway, Query, Submit, Validate}
import csw.command.api.scaladsl.CommandService
import csw.command.client.auth.CommandRoles
import csw.command.client.handlers.TestHelper.Narrower
import csw.commons.RandomUtils
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse.Invalid
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.Id
import csw.prefix.models.{Prefix, Subsystem}
import msocket.http.post.{PostRouteFactory, ServerHttpCodecs}
import msocket.jvm.metrics.LabelExtractor
import msocket.security.models.{Access, AccessToken}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, never, reset, verify, when}
import org.mockito.captor.{ArgCaptor, Captor}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

import scala.concurrent.Future

class CommandServiceHttpHandlerAuthTest
    extends AnyFunSuite
    with ScalatestRouteTest
    with Matchers
    with CommandServiceCodecs
    with ServerHttpCodecs
    with BeforeAndAfterEach {

  private val subsystem            = randomSubsystem
  private val prefix               = Prefix(subsystem, RandomUtils.randomString5())
  private val accessTokenDirective = BasicDirectives.extract(_ => accessToken)

  private val securityDirective = mock[SecurityDirectives]
  private val commandService    = mock[CommandService]
  private val accessToken       = mock[AccessToken]
  private val handler           = new CommandServiceRequestHandler(commandService, securityDirective, Some(prefix), CommandRoles.empty)

  import LabelExtractor.Implicits.default
  private val route = new PostRouteFactory[CommandServiceRequest]("post-endpoint", handler).make()

  override def beforeEach(): Unit = {
    reset(securityDirective, commandService, accessToken)
    super.beforeEach()
  }

  private val command = Setup(prefix, CommandName(RandomUtils.randomString5()), None)
  private val authEnabledRequests = Table[CommandServiceRequest, CommandService => Future[CommandResponse]](
    ("msg", "action"),
    (Submit(command), _.submit(command)),
    (Validate(command), _.validate(command)),
    (Oneway(command), _.oneway(command))
  )

  forAll(authEnabledRequests) { case (msg, action) =>
    val name = msg.getClass.getSimpleName

    test(s"$name should check for auth if destination prefix is provided while creating handlers") {
      val captor          = ArgCaptor[CustomPolicy]
      val invalidResponse = Invalid(Id(), IdNotAvailableIssue(RandomUtils.randomString5()))

      when(securityDirective.sPost(captor)).thenReturn(accessTokenDirective)
      when(action(commandService)).thenReturn(Future.successful(invalidResponse))

      Post("/post-endpoint", msg.narrow) ~> route ~> check {
        verify(securityDirective).sPost(any[CustomPolicy])
        checkCapturedPolicy(captor)
      }
    }
  }

  test(s"query should not check for auth") {
    val id      = Id()
    val invalid = Invalid(id, IdNotAvailableIssue(RandomUtils.randomString5()))
    when(commandService.query(id)).thenReturn(Future.successful(invalid))

    Post("/post-endpoint", Query(id).narrow) ~> route ~> check {
      verify(commandService).query(id)
      verify(securityDirective, never).sPost(any[CustomPolicy])
    }
  }

  private def checkCapturedPolicy(captor: Captor[CustomPolicy]) =
    captor.value.predicate(AccessToken(realm_access = Access(Set(s"$subsystem-user")))) should ===(true)

  private def randomSubsystem: Subsystem = RandomUtils.randomFrom(Subsystem.values)
}
