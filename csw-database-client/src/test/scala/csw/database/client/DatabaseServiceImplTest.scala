package csw.database.client

import akka.actor.ActorSystem
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
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
    databaseService.execute("DROP DATABASE box_office;").futureValue(Timeout(Span(5, Seconds)))
    databaseService.execute("DROP TABLE budget;").futureValue
    databaseService.execute("DROP TABLE films;").futureValue
    postgres.stop()
    system.terminate().futureValue
  }

  test("should be able to create a new Database") {
    databaseService.execute("CREATE DATABASE box_office;").futureValue(Timeout(Span(5, Seconds)))
    val resultSet = databaseService
      .executeQuery(
        """SELECT datname FROM pg_database WHERE datistemplate = false;"""
      )
      .futureValue
    val databaseList =
      Iterator.from(0).takeWhile(_ => resultSet.next()).map(_ => resultSet.getString(1)).toList
    assert(databaseList contains "box_office")
  }

  test("should be able to create table with unique default IDs and insert data in it") {
    databaseService
      .execute(
        "CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL);"
      )
      .futureValue
    databaseService.execute("INSERT INTO films VALUES (DEFAULT, 'movie_1');").futureValue
    databaseService.execute("INSERT INTO films VALUES (DEFAULT, 'movie_4');").futureValue
    databaseService.execute("INSERT INTO films VALUES (DEFAULT, 'movie_2');").futureValue
    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films;").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 3
  }

  test("should be able to create table with foreign key and insert data in it") {
    databaseService
      .execute(
        """CREATE TABLE budget (
          |id SERIAL PRIMARY KEY,
          |movie_id INTEGER,
          |movie_name VARCHAR(10),
          |amount NUMERIC,
          |FOREIGN KEY (movie_id) REFERENCES films(id) ON DELETE CASCADE
          |);""".stripMargin
      )
      .futureValue
    databaseService.execute("INSERT INTO budget VALUES (DEFAULT, 1, 'movie_1', 5000);").futureValue
    databaseService.execute("INSERT INTO budget VALUES (DEFAULT, 2, 'movie_4', 6000);").futureValue
    databaseService.execute("INSERT INTO budget VALUES (DEFAULT, 3, 'movie_2', 7000);").futureValue
    databaseService.execute("INSERT INTO budget VALUES (DEFAULT, 3, 'movie_2', 3000);").futureValue

    val resultSet =
      databaseService.executeQuery("""
          |SELECT films.name, SUM(budget.amount)
          |    FROM films
          |    INNER JOIN budget
          |    ON films.id = budget.movie_id
          |    GROUP BY  films.name;
        """.stripMargin).futureValue

    val result = Iterator
      .from(0)
      .takeWhile(_ => resultSet.next())
      .map { _ =>
        (resultSet.getString(1), resultSet.getInt(2))
      }
      .toList

    assert(result === List(("movie_1", 5000), ("movie_4", 6000), ("movie_2", 10000)))
  }

  test("should be able to update record") {
    databaseService.execute("UPDATE films SET name = 'movie_3' WHERE name = 'movie_2'").futureValue
    val resultSet =
      databaseService.executeQuery("SELECT count(*) AS rowCount from films where name = 'movie_2';").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 0
  }

  test("should be able to delete records") {
    databaseService.execute("DELETE from films WHERE name = 'movie_4'").futureValue
    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films;").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 2
  }

  test("should be able to query records from the table") {
    val resultSet = databaseService.executeQuery("SELECT * FROM films where name = 'movie_1';").futureValue
    resultSet.next()
    resultSet.getString("name").trim shouldEqual "movie_1"
  }
}
