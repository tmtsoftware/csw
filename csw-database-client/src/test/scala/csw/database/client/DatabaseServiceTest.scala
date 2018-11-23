package csw.database.client

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.scalatest.concurrent.ScalaFutures
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
  val factory                          = new DatabaseServiceFactory()
  val databaseService: DatabaseService = factory.make(postgres.getJdbcUrl("postgres", "postgres"))

  override def afterAll(): Unit = {
    databaseService.closeConnection()
    postgres.close()
    system.terminate().futureValue
  }

  //DEOPSCSW-608: Examples of database creation
  test("should be able to create a new Database") { databaseService: DatabaseService =>
    databaseService.execute("CREATE DATABASE box_office").futureValue

    val resultSet = databaseService.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false").futureValue

    val databaseList = Iterator.from(0).takeWhile(_ => resultSet.next()).map(_ => resultSet.getString(1)).toList
    databaseList should contain("box_office")

    databaseService.execute("DROP DATABASE box_office").futureValue

    val resultSet2    = databaseService.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false").futureValue
    val databaseList2 = Iterator.from(0).takeWhile(_ => resultSet2.next()).map(_ => resultSet2.getString(1)).toList
    databaseList2 should not contain "box_office"
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to alter/drop a table") {
    databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY)").futureValue
    val resultSetBeforeAlter = databaseService.executeQuery("SELECT * from films").futureValue
    val rsmd                 = resultSetBeforeAlter.getMetaData
    rsmd.getColumnCount shouldBe 1

    databaseService.execute("ALTER TABLE films ADD COLUMN name VARCHAR(10)").futureValue
    val resultSetAfterAlter = databaseService.executeQuery("SELECT * from films").futureValue
    val rsmdAltered         = resultSetAfterAlter.getMetaData
    rsmdAltered.getColumnCount shouldBe 2

    val tableResultSet = databaseService.executeQuery("select table_name from information_schema.tables").futureValue
    val tables         = Iterator.from(0).takeWhile(_ => tableResultSet.next()).map(_ => tableResultSet.getString(1)).toList

    tables should contain("films")

    databaseService.execute("DROP TABLE films").futureValue

    val tableResultSet2 = databaseService.executeQuery("select table_name from information_schema.tables").futureValue
    val tables2         = Iterator.from(0).takeWhile(_ => tableResultSet2.next()).map(_ => tableResultSet2.getString(1)).toList

    tables2 should not contain "new_table"
  }

  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
  //DEOPSCSW-610: Examples of Reading Records
  test("should be able to query records from the table") {
    // create films
    databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").futureValue

    val resultSet = databaseService.executeQuery("SELECT * FROM films where name = 'movie_1'").futureValue
    resultSet.next()
    resultSet.getString("name").trim shouldEqual "movie_1"

    databaseService.execute("DROP TABLE films").futureValue
  }

  //DEOPSCSW-609: Examples of Record creation
  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-610: Examples of Reading Records
  test("should be able to create table with unique default IDs and insert data in it") {
    // create films
    databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_4')").futureValue
    databaseService.execute("INSERT INTO films VALUES (DEFAULT, 'movie_2')").futureValue

    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 3

    databaseService.execute("DROP TABLE films").futureValue
  }

  //DEOPSCSW-607: Complex relational database example
  //DEOPSCSW-609: Examples of Record creation
  //DEOPSCSW-613: Examples of querying records
  //DEOPSCSW-610: Examples of Reading Records
  test("should be able to create, join and group records") { databaseService: DatabaseService =>
    // create films
    databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_4')").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").futureValue

    // create budget
    databaseService
      .execute(
        """CREATE TABLE budget (
            |id SERIAL PRIMARY KEY,
            |movie_id INTEGER,
            |movie_name VARCHAR(10),
            |amount NUMERIC,
            |FOREIGN KEY (movie_id) REFERENCES films(id) ON DELETE CASCADE
            |)""".stripMargin
      )
      .futureValue

    databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (1, 'movie_1', 5000)").futureValue
    databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (2, 'movie_4', 6000)").futureValue
    databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 7000)").futureValue
    databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 3000)").futureValue

    val resultSet =
      databaseService
        .executeQuery("""
              |SELECT films.name, SUM(budget.amount)
              |    FROM films
              |    INNER JOIN budget
              |    ON films.id = budget.movie_id
              |    GROUP BY  films.name
            """.stripMargin)
        .futureValue

    val result = Iterator
      .from(0)
      .takeWhile(_ => resultSet.next())
      .map { _ =>
        (resultSet.getString(1), resultSet.getInt(2))
      }
      .toSet

    result shouldEqual Set(("movie_1", 5000), ("movie_4", 6000), ("movie_2", 10000))

    databaseService.execute("DROP TABLE films").futureValue
    databaseService.execute("DROP TABLE box_office").futureValue
  }

  //DEOPSCSW-611: Examples of updating records
  //DEOPSCSW-619: Create a method to send an update sql string to a database
  test("should be able to update record") { databaseService: DatabaseService =>
    // create films
    databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").futureValue

    // update record
    databaseService.execute("UPDATE films SET name = 'movie_3' WHERE name = 'movie_2'").futureValue

    val resultSet =
      databaseService.executeQuery("SELECT count(*) AS rowCount from films where name = 'movie_2'").futureValue

    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 0

    databaseService.execute("DROP TABLE films").futureValue
  }

  //DEOPSCSW-612: Examples of deleting records
  test("should be able to delete records") {
    // create films
    databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_4')").futureValue
    databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").futureValue

    databaseService.execute("DELETE from films WHERE name = 'movie_4'").futureValue

    val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films").futureValue
    resultSet.next()
    resultSet.getInt("rowCount") shouldBe 2

    databaseService.execute("DROP TABLE films").futureValue
  }
}
