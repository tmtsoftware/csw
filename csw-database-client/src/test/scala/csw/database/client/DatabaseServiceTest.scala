package csw.database.client

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import slick.jdbc.PostgresProfile.api._

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
  val factory                          = new DatabaseServiceFactory()
  val databaseService: DatabaseService = factory.make("localhost", postgres.getPort, "postgres", "postgres")

  override def afterAll(): Unit = {
    postgres.close()
    system.terminate().futureValue
  }

  //DEOPSCSW-608: Examples of database creation
  test("should be able to create a new Database") {
    // create box_office database
    databaseService.update(sqlu"CREATE DATABASE box_office").futureValue(Interval(Span(5, Seconds)))

    // assert creation of database
    val getDatabases = sql"SELECT datname FROM pg_database WHERE datistemplate = false".as[String]
    val resultSet    = databaseService.query(getDatabases).futureValue
    resultSet should contain("box_office")

    // drop box_office database
    databaseService.update(sqlu"DROP DATABASE box_office").futureValue

    // assert removal of database
    val resultSet2 = databaseService.query(getDatabases).futureValue
    resultSet2 should not contain "box_office"
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to alter/drop a table") {
    // create films
    databaseService.update(sqlu"CREATE TABLE films (id SERIAL PRIMARY KEY)").futureValue

    // assert creation of table
    val getTables      = sql"select table_name from information_schema.tables".as[String]
    val tableResultSet = databaseService.query(getTables).futureValue
    tableResultSet should contain("films")

    // assert the count of the columns in films
    val getColumnCount       = sql"SELECT Count(*) FROM INFORMATION_SCHEMA.Columns where TABLE_NAME = 'films'".as[Int]
    val resultSetBeforeAlter = databaseService.query(getColumnCount).futureValue
    resultSetBeforeAlter.headOption shouldBe Some(1)

    // add one more column in films
    databaseService.update(sqlu"ALTER TABLE films ADD COLUMN name VARCHAR(10)").futureValue

    // assert increased count of column in films
    val resultSetAfterAlter = databaseService.query(getColumnCount).futureValue
    resultSetAfterAlter.headOption shouldBe Some(2)

    // drop table
    databaseService.update(sqlu"DROP TABLE films").futureValue
    val tableResultSet2 = databaseService.query(getTables).futureValue

    // assert removal of table
    tableResultSet2 should not contain "films"
  }

  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
  //DEOPSCSW-610: Examples of Reading Records
  //DEOPSCSW-609: Examples of Record creation
  test("should be able to query records from the table") {
    // create films and insert movie_1
    val movieName = "movie_1"
    databaseService
      .update(sqlu"CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)")
      .futureValue
    databaseService.update(sqlu"INSERT INTO films(name) VALUES ($movieName)").futureValue

    // query the table and assert on data received
    val resultSet: Seq[(Int, String)] =
      databaseService
        .query(sql"SELECT * FROM films where name = $movieName".as[(Int, String)])
        .futureValue
    resultSet.headOption shouldEqual Some((1, "movie_1"))

    databaseService.update(sqlu"DROP TABLE films").futureValue
  }

  //DEOPSCSW-607: Complex relational database example
  //DEOPSCSW-609: Examples of Record creation
  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-610: Examples of Reading Records
  test("should be able to create, join and group records") { databaseService: DatabaseService =>
    // create tables films and budget and insert records
    databaseService
      .updateAll(
        List(
          sqlu"CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)",
          sqlu"INSERT INTO films(name) VALUES ('movie_1')",
          sqlu"INSERT INTO films(name) VALUES ('movie_4')",
          sqlu"INSERT INTO films(name) VALUES ('movie_2')",
          sqlu"""
               CREATE TABLE budget (
               id SERIAL PRIMARY KEY,
               movie_id INTEGER,
               movie_name VARCHAR(10),
               amount NUMERIC,
               FOREIGN KEY (movie_id) REFERENCES films(id) ON DELETE CASCADE
               )""",
          sqlu"INSERT INTO budget(movie_id, movie_name, amount) VALUES (1, 'movie_1', 5000)",
          sqlu"INSERT INTO budget(movie_id, movie_name, amount) VALUES (2, 'movie_4', 6000)",
          sqlu"INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 7000)",
          sqlu"INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 3000)"
        )
      )
      .futureValue

    // query with joins and group by
    val resultSet =
      databaseService
        .query(sql"""
              |SELECT films.name, SUM(budget.amount)
              |    FROM films
              |    INNER JOIN budget
              |    ON films.id = budget.movie_id
              |    GROUP BY  films.name
            """.stripMargin.as[(String, Int)])
        .futureValue

    resultSet.toSet shouldEqual Set(("movie_1", 5000), ("movie_4", 6000), ("movie_2", 10000))

    databaseService.update(sqlu"DROP TABLE films").futureValue
    databaseService.update(sqlu"DROP TABLE box_office").futureValue
  }

  //DEOPSCSW-611: Examples of updating records
  //DEOPSCSW-619: Create a method to send an update sql string to a database
  test("should be able to update record") { databaseService: DatabaseService =>
    // create films and insert record
    val movie_2 = "movie_2"
    databaseService
      .updateAll(
        List(
          sqlu"CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)",
          sqlu"INSERT INTO films(name) VALUES ($movie_2)"
        )
      )
      .futureValue

    // update record value
    databaseService.update(sqlu"UPDATE films SET name = 'movie_3' WHERE name = $movie_2").futureValue

    // assert the record is updated
    val resultSet =
      databaseService
        .query(sql"SELECT count(*) AS rowCount from films where name = $movie_2".as[Int])
        .futureValue

    resultSet.headOption shouldBe Some(0)

    databaseService.update(sqlu"DROP TABLE films").futureValue
  }

//  //DEOPSCSW-612: Examples of deleting records
  test("should be able to delete records") {
    // create films and insert records
    val movie4 = "movie_4"
    databaseService
      .updateAll(
        List(
          sqlu"CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)",
          sqlu"INSERT INTO films(name) VALUES ('movie_1')",
          sqlu"INSERT INTO films(name) VALUES ($movie4)",
          sqlu"INSERT INTO films(name) VALUES ('movie_2')"
        )
      )
      .futureValue

    // delete movie_4
    databaseService.update(sqlu"DELETE from films WHERE name = $movie4").futureValue

    // assert the removal of record
    val resultSet =
      databaseService.query(sql"SELECT count(*) AS rowCount from films".as[Int]).futureValue
    resultSet.headOption shouldBe Some(2)

    databaseService.update(sqlu"DROP TABLE films").futureValue
  }
}
