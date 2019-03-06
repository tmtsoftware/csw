package csw.database

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import csw.database.scaladsl.JooqExtentions.{RichQuery, RichResultQuery}
import csw.database.commons.{DBTestHelper, DatabaseServiceConnection}
import csw.location.api.models.TcpRegistration
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

//DEOPSCSW-620: Session Creation to access data
//DEOPSCSW-621: Session creation to access data with single connection
//DEOPSCSW-615: DB service accessible to CSW component developers
class DatabaseServiceFactoryTest extends FunSuite with Matchers with BeforeAndAfterAll with HTTPLocationService {
  private implicit val system: ActorSystem  = ActorSystem("test")
  private implicit val ec: ExecutionContext = system.dispatcher
  private implicit val mat: Materializer    = ActorMaterializer()

  private val dbName: String                    = "postgres"
  private val port: Int                         = 5432
  private val locationService: LocationService  = HttpLocationServiceFactory.makeLocalClient
  private var postgres: EmbeddedPostgres        = _
  private var dbFactory: DatabaseServiceFactory = _
  private var testDsl: DSLContext               = _

  override def beforeAll(): Unit = {
    postgres = DBTestHelper.postgres(port)
    dbFactory = DBTestHelper.dbServiceFactory(system)
    testDsl = DBTestHelper.dslContext(system, port)
    // create a database
    testDsl.query("CREATE TABLE box_office(id SERIAL PRIMARY KEY)").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))

    super.beforeAll()
    locationService
      .register(TcpRegistration(DatabaseServiceConnection.value, port))
      .futureValue(Interval(Span(5, Seconds)))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testDsl.query("DROP TABLE box_office").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))
    postgres.close()
    system.terminate().futureValue
  }

  //DEOPSCSW-618: Integration with Location Service
  //DEOPSCSW-606: Examples for storing and using authentication information
  test("should create DSLContext using location service and dbName") {
    val dsl: DSLContext = dbFactory.makeDsl(locationService, dbName).futureValue(Interval(Span(5, Seconds)))

    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")
  }

  //DEOPSCSW-618: Integration with Location Service
  //DEOPSCSW-606: Examples for storing and using authentication information
  test("should create DSLContext using location service, dbName, usernameHolder and passwordHolder") {
    val dsl =
      dbFactory.makeDsl(locationService, dbName, ReadUsernameHolder, ReadPasswordHolder).futureValue(Interval(Span(5, Seconds)))

    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")
  }

  test("should create DSLContext using config") {
    val dsl = dbFactory.makeDsl().futureValue(Interval(Span(5, Seconds)))

    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")
  }

  //DEOPSCSW-605: Examples for multiple database support
  test("should be able to connect to other database") {
    // create a new database
    testDsl.query("CREATE DATABASE postgres2").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))

    // make connection to the new database
    val dsl =
      dbFactory.makeDsl(locationService, "postgres2").futureValue(Interval(Span(5, Seconds)))

    // assert that box_office table is not present in newly created db
    val resultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))
    resultSet should not contain "box_office"

    // create a new table box_office in newly created db
    dsl.query("CREATE TABLE box_office(id SERIAL PRIMARY KEY)").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))

    // assert the creation of table
    val resultSet2 = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet2 should contain("box_office")
  }
}
