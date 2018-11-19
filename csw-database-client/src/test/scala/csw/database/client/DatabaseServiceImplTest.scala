package csw.database.client

import java.sql.{Connection, DriverManager}

import akka.actor.ActorSystem
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.internal.DatabaseServiceImpl
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

class DatabaseServiceImplTest extends FunSuite with Matchers with BeforeAndAfterAll {
  private val system                = ActorSystem("test")
  implicit val ec: ExecutionContext = system.dispatcher

  private val host   = "localhost"
  private val port   = 5433
  private val dbName = "test"

  private val postgres: EmbeddedPostgres = new EmbeddedPostgres(Version.V10_6)
  postgres.start(host, port, dbName)

  private val factory                          = new DatabaseServiceFactory()
  private val databaseService: DatabaseService = factory.make(host, port, dbName)

  override def afterAll(): Unit = {
    postgres.stop()
    Await.result(system.terminate(), 5.seconds)
  }

  test("should be able to create table and insert data in it") {
    Await.result(databaseService.execute("CREATE TABLE films (code char(10));"), 5.seconds)
    Await.result(databaseService.execute("INSERT INTO films VALUES ('movie_1');"), 5.seconds)
    Await.result(databaseService.execute("INSERT INTO films VALUES ('movie_4');"), 5.seconds)
    Await.result(databaseService.execute("INSERT INTO films VALUES ('movie_2');"), 5.seconds)

    val resultSet = Await.result(databaseService.executeQuery("SELECT count(*) AS rowCount from films;"), 5.seconds)
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 3

  }

  test("should be able to update record") {
    Await.result(databaseService.execute("UPDATE films SET code = 'movie_3' WHERE code = 'movie_2'"), 5.seconds)

    val resultSet =
      Await.result(databaseService.executeQuery("SELECT count(*) AS rowCount from films where code = 'movie_2';"), 5.seconds)
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 0
  }

  test("should be able to delete records") {
    Await.result(databaseService.execute("DELETE from films WHERE code = 'movie_4'"), 5.seconds)
    val resultSet = Await.result(databaseService.executeQuery("SELECT count(*) AS rowCount from films;"), 5.seconds)
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 2
  }

  test("should be able to query records from the table") {
    val resultSet = Await.result(databaseService.executeQuery("SELECT * FROM films where code = 'movie_1';"), 5.seconds)

    resultSet.next()
    resultSet.getString("code").trim shouldEqual "movie_1"
  }
}
