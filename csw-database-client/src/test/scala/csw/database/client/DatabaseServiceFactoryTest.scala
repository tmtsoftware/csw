package csw.database.client

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.client.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import csw.database.client.commons.DatabaseServiceConnection
import csw.database.client.scaladsl.JooqExtentions.{RichQuery, RichResultQuery}
import csw.database.commons.DBTestHelper
import csw.location.api.models.TcpRegistration
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

class DatabaseServiceFactoryTest extends FunSuite with Matchers with BeforeAndAfterAll with HTTPLocationService {

  private implicit val system: ActorSystem  = ActorSystem("test")
  private implicit val ec: ExecutionContext = system.dispatcher
  private implicit val mat: Materializer    = ActorMaterializer()

  private val locationService: LocationService  = HttpLocationServiceFactory.makeLocalClient
  private val postgres: EmbeddedPostgres        = DBTestHelper.postgres()
  private val port: Int                         = postgres.getPort
  private val dbName: String                    = "postgres"
  private val dbFactory: DatabaseServiceFactory = DBTestHelper.dbServiceFactory(system)

  // create a database
  private val testDsl: DSLContext = DBTestHelper.dslContext(system, port)
  testDsl.query("CREATE DATABASE box_office").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService
      .register(TcpRegistration(DatabaseServiceConnection.value, port))
      .futureValue(Interval(Span(5, Seconds)))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testDsl.query("DROP DATABASE box_office").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))
    postgres.close()
    system.terminate().futureValue
  }

  test("should create DSLContext using location service and dbName") {
    val dsl: DSLContext = dbFactory.makeDsl(locationService, dbName).futureValue(Interval(Span(5, Seconds)))

    val resultSet = dsl
      .resultQuery("SELECT datname FROM pg_database WHERE datistemplate = false")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")
  }

  test("should create DSLContext using location service, dbName, usernameHolder and passwordHolder") {
    val dsl =
      dbFactory.makeDsl(locationService, dbName, ReadUsernameHolder, ReadPasswordHolder).futureValue(Interval(Span(5, Seconds)))

    val resultSet = dsl
      .resultQuery("SELECT datname FROM pg_database WHERE datistemplate = false")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")
  }
}
