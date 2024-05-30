/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.database

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.commons.DBTestHelper
import csw.database.exceptions.DatabaseException
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

//DEOPSCSW-615: DB service accessible to CSW component developers
class DatabaseServiceFactoryFailureTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  private val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  private var postgres: EmbeddedPostgres                 = scala.compiletime.uninitialized
  private var dbFactory: DatabaseServiceFactory          = scala.compiletime.uninitialized

  override def beforeAll(): Unit = {
    postgres = DBTestHelper.postgres(0)
    dbFactory = DBTestHelper.dbServiceFactory(system)
  }

  override def afterAll(): Unit = {
    postgres.close()
    system.terminate()
    system.whenTerminated.futureValue
  }

  test("should throw DatabaseConnection while connecting with incorrect port | DEOPSCSW-615") {
    intercept[DatabaseException] {
      // postgres starts on random port but while connecting it tries to look at 5432
      Await.result(dbFactory.makeDsl(), 5.seconds)
    }
  }
}
