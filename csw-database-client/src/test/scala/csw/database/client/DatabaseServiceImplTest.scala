package csw.database.client

import java.sql.{Connection, DriverManager}

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

class DatabaseServiceImplTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val system                = ActorSystem("test")
  implicit val ec: ExecutionContext = system.dispatcher

  private val postgres: EmbeddedPostgres           = new EmbeddedPostgres(Version.V10_6)
  private val url: String                          = postgres.start("localhost", 5433, "test")
  private val conn: Connection                     = DriverManager.getConnection(url)
  private val databaseService: DatabaseServiceImpl = new DatabaseServiceImpl(conn)

  override def afterAll(): Unit = {
    conn.close()
    postgres.stop()
    Await.result(system.terminate(), 5.seconds)
  }

  test("testExecute for creating records in table films") {
    Await.result(databaseService.execute("CREATE TABLE films (code char(10));"), 5.seconds)
    Await.result(databaseService.execute("INSERT INTO films VALUES ('movie_1');"), 5.seconds)
    Await.result(databaseService.execute("INSERT INTO films VALUES ('movie_4');"), 5.seconds)
    Await.result(databaseService.execute("INSERT INTO films VALUES ('movie_2');"), 5.seconds)

    val resultSet = Await.result(databaseService.executeQuery("SELECT count(*) AS rowCount from films;"), 5.seconds)
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 3

  }

  test("testExecute for updating records within table films") {
    Await.result(databaseService.execute("UPDATE films SET code = 'movie_3' WHERE code = 'movie_2'"), 5.seconds)

    val resultSet =
      Await.result(databaseService.executeQuery("SELECT count(*) AS rowCount from films where code = 'movie_2';"), 5.seconds)
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 0
  }

  test("testExecute for deleting records from table films") {
    Await.result(databaseService.execute("DELETE from films WHERE code = 'movie_4'"), 5.seconds)
    val resultSet = Await.result(databaseService.executeQuery("SELECT count(*) AS rowCount from films;"), 5.seconds)
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 2
  }

  test("testExecuteQuery to query records from the table films") {
    val resultSet = Await.result(databaseService.executeQuery("SELECT * FROM films where code = 'movie_1';"), 5.seconds)

    resultSet.next()
    resultSet.getString("code").trim shouldEqual "movie_1"
  }
}
