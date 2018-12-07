package csw.database.client

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.typesafe.config.{Config, ConfigFactory}
import csw.database.client.scaladsl.JooqExtentions.{RichQuery, RichResultQuery}
import csw.database.client.scaladsl.DatabaseService
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

//DEOPSCSW-601: Create Database API
class DatabaseServiceTest extends FunSuite with Matchers with ScalaFutures with BeforeAndAfterAll {
  private val system: ActorSystem   = ActorSystem("test")
  implicit val ec: ExecutionContext = system.dispatcher

  val postgres: EmbeddedPostgres = EmbeddedPostgres.builder
    .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
    .setCleanDataDirectory(true)
    .setPgBinaryResolver(new PostgresBinaryResolver)
    .start

  //DEOPSCSW-620: Session Creation to access data
  private val portConfig     = s"csw-database.hikari-datasource.dataSource.portNumber = ${postgres.getPort}"
  private val config: Config = ConfigFactory.parseString(portConfig).withFallback(system.settings.config)

  val dsl: DSLContext = new DatabaseService(config).defaultDSL

  override def afterAll(): Unit = {
    postgres.close()
    system.terminate().futureValue
  }

  //DEOPSCSW-608: Examples of database creation
  test("should be able to create a new Database") {
    // create box_office database
    dsl
      .query("CREATE DATABASE box_office")
      .executeAsyncScala()
      .futureValue(Interval(Span(5, Seconds)))

    // assert creation of database
    val getDatabaseQuery = dsl.resultQuery("SELECT datname FROM pg_database WHERE datistemplate = false")

    val resultSet = getDatabaseQuery.fetchAsyncScala[String].futureValue(Interval(Span(5, Seconds)))

    resultSet should contain("box_office")

    // drop box_office database
    dsl
      .query("DROP DATABASE box_office")
      .executeAsyncScala()
      .futureValue(Interval(Span(5, Seconds)))

    // assert removal of database
    val resultSet2 = getDatabaseQuery.fetchAsyncScala[String].futureValue
    resultSet2 should not contain "box_office"
  }
}
