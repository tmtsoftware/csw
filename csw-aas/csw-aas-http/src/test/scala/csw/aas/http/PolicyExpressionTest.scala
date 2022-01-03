package csw.aas.http

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.*
import csw.aas.http.AuthorizationPolicy.{CustomPolicy, RealmRolePolicy}
import msocket.security.models.AccessToken
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class PolicyExpressionTest
    extends AnyFunSuite
    with MockitoSugar
    with Directives
    with ScalatestRouteTest
    with Matchers
    with ScalaFutures {

  case class TestCase(left: Boolean, operator: String, right: Boolean, expectedOutcome: Boolean)

  private implicit val patience: PatienceConfig = PatienceConfig(1.seconds, 100.millis)

  val testCases = List(
    //AND truth table
    TestCase(left = true, "And", right = true, expectedOutcome = true),
    TestCase(left = true, "And", right = false, expectedOutcome = false),
    TestCase(left = false, "And", right = true, expectedOutcome = false),
    TestCase(left = false, "And", right = false, expectedOutcome = false),
    //OR truth table
    TestCase(left = true, "Or", right = true, expectedOutcome = true),
    TestCase(left = true, "Or", right = false, expectedOutcome = true),
    TestCase(left = false, "Or", right = true, expectedOutcome = true),
    TestCase(left = false, "Or", right = false, expectedOutcome = false)
  )

  testCases.foreach(testCase => {
    import testCase._
    test(s"$left $operator $right = $expectedOutcome") {
      val token = mock[AccessToken]

      when(token.hasRealmRole("admin")).thenReturn(left)
      when(token.clientId).thenReturn(if (right) Some("abc") else None)

      val policyExpression = operator match {
        case "And" => RealmRolePolicy("admin") & CustomPolicy(_.clientId.isDefined)
        case "Or"  => RealmRolePolicy("admin") | CustomPolicy(_.clientId.isDefined)
      }

      policyExpression.authorize(token).futureValue shouldBe expectedOutcome

      verify(token).clientId
      verify(token).hasRealmRole("admin")
    }
  })

}
