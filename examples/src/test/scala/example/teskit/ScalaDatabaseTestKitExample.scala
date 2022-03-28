/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.teskit

import csw.testkit.scaladsl.CSWService.DatabaseServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

//#CSW-161
//#scalatest-database-testkit
class ScalaDatabaseTestKitExample extends ScalaTestFrameworkTestKit(DatabaseServer) with AnyFunSuiteLike {
  import frameworkTestKit.databaseTestKit.*

  test("test using dsl context") {
    val queryDsl = dslContext()
    // .. queries, assertions etc.
  }

  test("test using database service factory") {
    // Usage of Await.result is fine in test scope here, as `databaseServiceFactory().makeDsl()` returns Future.
    val queryDsl = Await.result(databaseServiceFactory().makeDsl(frameworkTestKit.locationService, "postgres"), 2.seconds)
    // .. queries, assertions etc.
  }
}
//#scalatest-database-testkit
