package example.teskit

import csw.testkit.scaladsl.CSWService.DatabaseServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

//#scalatest-database-testkit
class ScalaDatabaseTestKitExampleTest extends ScalaTestFrameworkTestKit(DatabaseServer) with AnyFunSuiteLike {
  import frameworkTestKit.databaseTestKit.*

  test("test using dsl context") {
    val queryDsl = dslContext()
    // .. queries, assertions etc.
  }

  test("test using database service factory") {
    val queryDsl = Await.result(databaseServiceFactory().makeDsl(frameworkTestKit.locationService, "postgres"), 2.seconds)
    // .. queries, assertions etc.
  }
}
//#scalatest-database-testkit
