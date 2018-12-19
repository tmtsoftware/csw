package csw.database.client

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
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

  System.setProperty("hostname", "localhost") // set localhost for location service to register postgres on localhost
  private val locationService: LocationService  = HttpLocationServiceFactory.makeLocalClient
  private val postgres: EmbeddedPostgres        = DBTestHelper.postgres()
  private val dsl: DSLContext                   = DBTestHelper.dslContext(system, postgres.getPort)
  private val dbFactory: DatabaseServiceFactory = DBTestHelper.dbServiceFactory(system)

  // create a database
  dsl.query("CREATE DATABASE box_office").executeAsyncScala().futureValue(Interval(Span(5, Seconds)))

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService
      .register(TcpRegistration(DatabaseServiceConnection.value, postgres.getPort))
      .futureValue(Interval(Span(5, Seconds)))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    postgres.close()
    System.clearProperty("hostname")
    system.terminate().futureValue
  }

  test("should create DSLContext using location service") {
    val dsl: DSLContext = dbFactory.makeDsl(locationService, "postgres").futureValue(Interval(Span(5, Seconds)))

    val resultSet = dsl
      .resultQuery("SELECT datname FROM pg_database WHERE datistemplate = false")
      .fetchAsyncScala[String]
      .futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")
  }
}
