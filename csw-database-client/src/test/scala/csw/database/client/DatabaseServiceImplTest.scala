package csw.database.client

import java.nio.file.Paths
import java.util.Collections

import akka.actor.ActorSystem
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.junit.Assert
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

import scala.concurrent.ExecutionContext

//DEOPSCSW-601: Create Database API
class DatabaseServiceImplTest extends FunSuite with Matchers with ScalaFutures with BeforeAndAfterAll {
  private val system                = ActorSystem("test")
  implicit val ec: ExecutionContext = system.dispatcher

  private val postgres: EmbeddedPostgres = new EmbeddedPostgres(Version.V10_6, "/tmp/postgresDataDir")
  postgres.start(EmbeddedPostgres.cachedRuntimeConfig(Paths.get("/tmp/postgresExtracted")))

  //DEOPSCSW-618: Create a method to locate a database server
  //DEOPSCSW-620: Create a method to make a connection to a database
  //DEOPSCSW-621: Create a session with a database
  private val factory                          = new DatabaseServiceFactory()
  private val databaseService: DatabaseService = factory.make(postgres.getConnectionUrl.get())

  override def afterAll(): Unit = {
    databaseService.execute("DROP DATABASE box_office_scala;").futureValue(Timeout(Span(5, Seconds)))
    databaseService.execute("DROP TABLE budget_scala;").futureValue
    databaseService.execute("DROP TABLE films_scala;").futureValue
    databaseService.execute("DROP TABLE new_table_scala;").futureValue
    postgres.stop()
    system.terminate().futureValue
  }

  //DEOPSCSW-608: Examples of creating a database in the database service
  test("should be able to create a new Database") {
    databaseService.execute("CREATE DATABASE box_office_scala;").futureValue(Timeout(Span(5, Seconds)))

    val resultSet = databaseService
      .executeQuery("""SELECT datname FROM pg_database WHERE datistemplate = false;""")
      .futureValue

    val databaseList = Iterator.from(0).takeWhile(_ => resultSet.next()).map(_ => resultSet.getString(1)).toList
    assert(databaseList contains "box_office_scala")
  }

  //DEOPSCSW-609: Examples of creating records in a database in the database service
  //DEOPSCSW-613: Examples of querying records in a database in the database service
  test("should be able to create table with unique default IDs and insert data in it") {
    databaseService
      .execute("CREATE TABLE films_scala (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL);")
      .futureValue(Timeout(Span(5, Seconds)))

    databaseService.execute("INSERT INTO films_scala VALUES (DEFAULT, 'movie_1');").futureValue
    databaseService.execute("INSERT INTO films_scala VALUES (DEFAULT, 'movie_4');").futureValue
    databaseService.execute("INSERT INTO films_scala VALUES (DEFAULT, 'movie_2');").futureValue

    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films_scala;").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 3
  }

  //DEOPSCSW-607: Complex relational database example
  //DEOPSCSW-609: Examples of creating records in a database in the database service
  //DEOPSCSW-613: Examples of querying records in a database in the database service
  test("should be able to create join and group records using ForeignKey") {
    databaseService
      .execute(
        """CREATE TABLE budget_scala (
          |id SERIAL PRIMARY KEY,
          |movie_id INTEGER,
          |movie_name VARCHAR(10),
          |amount NUMERIC,
          |FOREIGN KEY (movie_id) REFERENCES films_scala(id) ON DELETE CASCADE
          |);""".stripMargin
      )
      .futureValue
    databaseService.execute("INSERT INTO budget_scala VALUES (DEFAULT, 1, 'movie_1', 5000);").futureValue
    databaseService.execute("INSERT INTO budget_scala VALUES (DEFAULT, 2, 'movie_4', 6000);").futureValue
    databaseService.execute("INSERT INTO budget_scala VALUES (DEFAULT, 3, 'movie_2', 7000);").futureValue
    databaseService.execute("INSERT INTO budget_scala VALUES (DEFAULT, 3, 'movie_2', 3000);").futureValue

    val resultSet =
      databaseService
        .executeQuery("""
          |SELECT films_scala.name, SUM(budget_scala.amount)
          |    FROM films_scala
          |    INNER JOIN budget_scala
          |    ON films_scala.id = budget_scala.movie_id
          |    GROUP BY  films_scala.name;
        """.stripMargin)
        .futureValue

    val result = Iterator
      .from(0)
      .takeWhile(_ => resultSet.next())
      .map { _ =>
        (resultSet.getString(1), resultSet.getInt(2))
      }
      .toSet

    assert(result === Set(("movie_1", 5000), ("movie_4", 6000), ("movie_2", 10000)))
  }

  //DEOPSCSW-611: Examples of updating records in a database in the database service
  //DEOPSCSW-619: Create a method to send an update sql string to a database
  test("should be able to update record") {
    databaseService.execute("UPDATE films_scala SET name = 'movie_3' WHERE name = 'movie_2'").futureValue
    val resultSet =
      databaseService.executeQuery("SELECT count(*) AS rowCount from films_scala where name = 'movie_2';").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 0
  }

  //DEOPSCSW-612: Examples of deleting records in a database in the database service
  test("should be able to delete records") {
    databaseService.execute("DELETE from films_scala WHERE name = 'movie_4'").futureValue
    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films_scala;").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 2
  }

  //DEOPSCSW-613: Examples of querying records in a database in the database service
  //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
  test("should be able to query records from the table") {
    val resultSet = databaseService.executeQuery("SELECT * FROM films_scala where name = 'movie_1';").futureValue
    resultSet.next()
    resultSet.getString("name").trim shouldEqual "movie_1"
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to drop a table") {
    val dbm = databaseService.getConnectionMetaData.futureValue
    databaseService.execute("CREATE TABLE table_scala (id SERIAL PRIMARY KEY);").futureValue
    val tableCheckAfterCreate = dbm.getTables(null, null, "table_scala", null)
    Assert.assertTrue(tableCheckAfterCreate.next)

    databaseService.execute("DROP TABLE table_scala;").futureValue
    val tableCheckAfterDrop = dbm.getTables(null, null, "table_scala", null)
    Assert.assertFalse(tableCheckAfterDrop.next)
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to alter a table") {
    databaseService.execute("CREATE TABLE new_table_scala (id SERIAL PRIMARY KEY);").futureValue
    val resultSetBeforeAlter = databaseService.executeQuery("SELECT * from new_table_scala;").futureValue
    val rsmd                 = resultSetBeforeAlter.getMetaData
    Assert.assertEquals(1, rsmd.getColumnCount)

    databaseService.execute("ALTER TABLE new_table_scala ADD COLUMN name VARCHAR(10);").futureValue
    val resultSetAfterAlter = databaseService.executeQuery("SELECT * from new_table_scala;").futureValue
    val rsmdAltered         = resultSetAfterAlter.getMetaData
    Assert.assertEquals(2, rsmdAltered.getColumnCount)
  }
}
