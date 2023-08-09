/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.database

import org.apache.pekko.actor.typed
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import csw.database.commons.{DBTestHelper, DatabaseServiceConnection}
import csw.database.scaladsl.JooqExtentions.{RichQuery, RichResultQuery}
import csw.location.api.models
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.jooq.DSLContext
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

//DEOPSCSW-620: Session Creation to access data
//DEOPSCSW-621: Session creation to access data with single connection
//DEOPSCSW-615: DB service accessible to CSW component developers
class DatabaseServiceFactoryTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with HTTPLocationService {
  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "test")
  private implicit val ec: ExecutionContext                            = typedSystem.executionContext

  private val dbName: String                    = "postgres"
  private val port: Int                         = 5432
  private val locationService: LocationService  = HttpLocationServiceFactory.makeLocalClient
  private var postgres: EmbeddedPostgres        = _
  private var dbFactory: DatabaseServiceFactory = _
  private var testDsl: DSLContext               = _

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds)

  override def beforeAll(): Unit = {
    postgres = DBTestHelper.postgres(port)
    dbFactory = DBTestHelper.dbServiceFactory(typedSystem)
    testDsl = DBTestHelper.dslContext(typedSystem, port)
    // create a database
    testDsl.query("CREATE TABLE box_office(id SERIAL PRIMARY KEY)").executeAsyncScala().futureValue

    super.beforeAll()
    locationService
      .register(models.TcpRegistration(DatabaseServiceConnection.value, port))
      .futureValue
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testDsl.query("DROP TABLE box_office").executeAsyncScala().futureValue
    postgres.close()
    typedSystem.terminate()
    typedSystem.whenTerminated.futureValue
  }

  // DEOPSCSW-618: Integration with Location Service
  // DEOPSCSW-606: Examples for storing and using authentication information
  test(
    "should create DSLContext using location service and dbName | DEOPSCSW-618, DEOPSCSW-621, DEOPSCSW-615, DEOPSCSW-620, DEOPSCSW-606"
  ) {
    val dsl: DSLContext = dbFactory.makeDsl(locationService, dbName).futureValue

    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue

    resultSet should contain("box_office")
  }

  // DEOPSCSW-618: Integration with Location Service
  // DEOPSCSW-606: Examples for storing and using authentication information
  test(
    "should create DSLContext using location service, dbName, usernameHolder and passwordHolder | DEOPSCSW-618, DEOPSCSW-621, DEOPSCSW-615, DEOPSCSW-620, DEOPSCSW-606"
  ) {
    val dsl =
      dbFactory.makeDsl(locationService, dbName, ReadUsernameHolder, ReadPasswordHolder).futureValue

    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue

    resultSet should contain("box_office")
  }

  test("should create DSLContext using config | DEOPSCSW-620, DEOPSCSW-621, DEOPSCSW-615") {
    val dsl = dbFactory.makeDsl().futureValue

    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue

    resultSet should contain("box_office")
  }

  // DEOPSCSW-605: Examples for multiple database support
  test("should be able to connect to other database | DEOPSCSW-620, DEOPSCSW-621, DEOPSCSW-615, DEOPSCSW-605") {
    // create a new database
    testDsl.query("CREATE DATABASE postgres2").executeAsyncScala().futureValue

    // make connection to the new database
    val dsl =
      dbFactory.makeDsl(locationService, "postgres2").futureValue

    // assert that box_office table is not present in newly created db
    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue
    resultSet should not contain "box_office"

    // create a new table box_office in newly created db
    dsl.query("CREATE TABLE box_office(id SERIAL PRIMARY KEY)").executeAsyncScala().futureValue

    // assert the creation of table
    val resultSet2 = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue

    resultSet2 should contain("box_office")
  }
}
