package csw.database.client

import akka.actor.ActorSystem
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.client.scaladsl.JooqExtentions.{RichQueries, RichQuery, RichResultQuery}
import csw.database.commons.DBTestHelper
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

//DEOPSCSW-601: Create Database API
//DEOPSCSW-616: Create a method to send a query (select) sql string to a database
class DatabaseServiceTest extends FunSuite with Matchers with ScalaFutures with BeforeAndAfterAll {
  private val system: ActorSystem           = ActorSystem("test")
  private implicit val ec: ExecutionContext = system.dispatcher
  private val postgres: EmbeddedPostgres    = DBTestHelper.postgres(0) // 0 is random port
  private val dsl: DSLContext               = DBTestHelper.dslContext(system, postgres.getPort)

  override def afterAll(): Unit = {
    postgres.close()
    system.terminate().futureValue
  }

  //DEOPSCSW-608: Examples of database creation
  test("should be able to create a new Database") {
    // ensure database isn't already present
    val getDatabaseQuery = dsl.resultQuery("SELECT datname FROM pg_database WHERE datistemplate = false")

    val resultSet = getDatabaseQuery.fetchAsyncScala[String].futureValue(Interval(Span(5, Seconds)))

    if (resultSet contains "box_office") {
      // drop box_office database
      dsl
        .query("DROP DATABASE box_office")
        .executeAsyncScala()
        .futureValue(Interval(Span(5, Seconds)))
    }

    // create box_office database
    dsl
      .query("CREATE DATABASE box_office")
      .executeAsyncScala()
      .futureValue(Interval(Span(5, Seconds)))

    // assert creation of database
    val resultSet2 = getDatabaseQuery.fetchAsyncScala[String].futureValue(Interval(Span(5, Seconds)))

    resultSet2 should contain("box_office")

    // drop box_office database
    dsl
      .query("DROP DATABASE box_office")
      .executeAsyncScala()
      .futureValue(Interval(Span(5, Seconds)))

    // assert removal of database
    val resultSet3 = getDatabaseQuery.fetchAsyncScala[String].futureValue
    resultSet3 should not contain "box_office"
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to alter/drop a table") {
    // create films
    dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY)").executeAsyncScala().futureValue

    val tableResultSet = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue

    tableResultSet should contain("films")

    val resultSetBeforeAlter = dsl
      .resultQuery("SELECT Count(*) FROM INFORMATION_SCHEMA.Columns where TABLE_NAME = 'films'")
      .fetchAsyncScala[Int]
      .futureValue

    resultSetBeforeAlter.headOption shouldBe Some(1)

    // add one more column in films
    dsl.query("ALTER TABLE films ADD COLUMN name VARCHAR(10)").executeAsyncScala().futureValue

    // assert increased count of column in films
    val resultSetAfterAlter = dsl
      .resultQuery("SELECT Count(*) FROM INFORMATION_SCHEMA.Columns where TABLE_NAME = 'films'")
      .fetchAsyncScala[Int]
      .futureValue

    resultSetAfterAlter.headOption shouldBe Some(2)

    // drop table
    dsl.query("DROP TABLE films").executeAsyncScala().futureValue
    val tableResultSet2 = dsl
      .resultQuery("select table_name from information_schema.tables")
      .fetchAsyncScala[String]
      .futureValue

    // assert removal of table
    tableResultSet2 should not contain "films"
  }

  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
  //DEOPSCSW-610: Examples of Reading Records
  //DEOPSCSW-609: Examples of Record creation
  test("should be able to query records from the table") {
    // create films and insert movie_1
    val movieName  = "movie_1"
    val movieName2 = "movie_2"
    dsl
      .query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)")
      .executeAsyncScala()
      .futureValue

    dsl.query("INSERT INTO films(name) VALUES (?)", movieName).executeAsyncScala().futureValue
    dsl.query("INSERT INTO films(name) VALUES (?)", movieName2).executeAsyncScala().futureValue

    // query the table and assert on data received
    val resultSet: Seq[(Int, String)] =
      dsl
        .resultQuery("SELECT * FROM films where name = ?", movieName)
        .fetchAsyncScala[(Int, String)]
        .futureValue

    resultSet shouldEqual Seq((1, "movie_1"))

    dsl.query("DROP TABLE films").executeAsyncScala().futureValue
  }

  //DEOPSCSW-607: Complex relational database example
  //DEOPSCSW-609: Examples of Record creation
  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-610: Examples of Reading Records
  test("should be able to create, join and group records") {
    // create tables films and budget and insert records
    dsl
      .queries(
        dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)"),
        dsl.query("INSERT INTO films(name) VALUES ('movie_1')"),
        dsl.query("INSERT INTO films(name) VALUES ('movie_4')"),
        dsl.query("INSERT INTO films(name) VALUES ('movie_2')"),
        dsl.query("""
                    |CREATE TABLE budget (
                    |               id SERIAL PRIMARY KEY,
                    |               movie_id INTEGER,
                    |               movie_name VARCHAR(10),
                    |               amount INTEGER,
                    |               FOREIGN KEY (movie_id) REFERENCES films(id) ON DELETE CASCADE
                    |               )
          """.stripMargin),
        dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (1, 'movie_1', 5000)"),
        dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (2, 'movie_4', 6000)"),
        dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 7000)"),
        dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 3000)")
      )
      .executeBatchAsync()
      .futureValue

    // query with joins and group by
    val resultSet =
      dsl
        .resultQuery("""
                    |SELECT films.name, SUM(budget.amount)
                    |    FROM films
                    |    INNER JOIN budget
                    |    ON films.id = budget.movie_id
                    |    GROUP BY  films.name
            """.stripMargin)
        .fetchAsyncScala[(String, Int)]
        .futureValue

    resultSet.toSet shouldBe Set(("movie_1", 5000), ("movie_2", 10000), ("movie_4", 6000))

    dsl.query("DROP TABLE budget").executeAsyncScala().futureValue
    dsl.query("DROP TABLE films").executeAsyncScala().futureValue
  }

  //DEOPSCSW-611: Examples of updating records
  //DEOPSCSW-619: Create a method to send an update sql string to a database
  test("should be able to update record") {
    // create films and insert record
    val movie_2 = "movie_2"
    dsl
      .queries(
        dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)"),
        dsl.query("INSERT INTO films(name) VALUES (?)", movie_2)
      )
      .executeBatchAsync()
      .futureValue

    // update record value
    dsl.query("UPDATE films SET name = 'movie_3' WHERE name = ?", movie_2).executeAsyncScala().futureValue

    // assert the record is updated
    val resultSet =
      dsl
        .resultQuery("SELECT count(*) AS rowCount from films where name = ?", movie_2)
        .fetchAsyncScala[Int]
        .futureValue

    resultSet.headOption shouldBe Some(0)

    dsl.query("DROP TABLE films").executeAsyncScala().futureValue
  }

  //DEOPSCSW-612: Examples of deleting records
  test("should be able to delete records") {
    // create films and insert records
    val movie4 = "movie_4"
    dsl
      .queries(
        dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)"),
        dsl.query("INSERT INTO films(name) VALUES ('movie_1')"),
        dsl.query("INSERT INTO films(name) VALUES (?)", movie4),
        dsl.query("INSERT INTO films(name) VALUES ('movie_2')")
      )
      .executeBatchAsync()
      .futureValue

    val resultSet =
      dsl.resultQuery("SELECT name from films where name=?", movie4).fetchAsyncScala[String].futureValue
    resultSet shouldBe Seq(movie4)

    // delete movie_4
    dsl.query("DELETE from films WHERE name = ?", movie4).executeAsyncScala().futureValue

    // assert the removal of record
    val resultSet2 =
      dsl.resultQuery("SELECT name from films where name=?", movie4).fetchAsyncScala[String].futureValue
    resultSet2 shouldBe Seq.empty

    dsl.query("DROP TABLE films").executeAsyncScala().futureValue
  }
}
