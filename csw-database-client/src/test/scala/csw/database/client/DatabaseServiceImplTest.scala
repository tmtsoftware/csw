package csw.database.client

import akka.actor.ActorSystem
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

import scala.concurrent.ExecutionContext

class DatabaseServiceImplTest extends FunSuite with Matchers with ScalaFutures with BeforeAndAfterAll {
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
    databaseService.execute("DROP TABLE films;").futureValue
    postgres.stop()
    system.terminate().futureValue
  }

  test("should be able to create table and insert data in it") {
    databaseService.execute("CREATE TABLE films (code char(10));").futureValue
    databaseService.execute("INSERT INTO films VALUES ('movie_1');").futureValue
    databaseService.execute("INSERT INTO films VALUES ('movie_4');").futureValue
    databaseService.execute("INSERT INTO films VALUES ('movie_2');").futureValue
    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films;").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 3

  }

  test("should be able to update record") {
    databaseService.execute("UPDATE films SET code = 'movie_3' WHERE code = 'movie_2'").futureValue
    val resultSet =
      databaseService.executeQuery("SELECT count(*) AS rowCount from films where code = 'movie_2';").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 0
  }

  test("should be able to delete records") {
    databaseService.execute("DELETE from films WHERE code = 'movie_4'").futureValue
    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films;").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 2
  }

  test("should be able to query records from the table") {
    val resultSet = databaseService.executeQuery("SELECT * FROM films where code = 'movie_1';").futureValue
    resultSet.next()
    resultSet.getString("code").trim shouldEqual "movie_1"
  }
}
