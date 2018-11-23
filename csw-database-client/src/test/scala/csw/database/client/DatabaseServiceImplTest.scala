package csw.database.client

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.api.scaladsl.DatabaseService
import csw.database.client.scaladsl.DatabaseServiceFactory
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Assertion, BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

//DEOPSCSW-601: Create Database API
class DatabaseServiceImplTest extends FunSuite with Matchers with ScalaFutures with BeforeAndAfterAll {
  private val system: ActorSystem              = ActorSystem("test")
  implicit val ec: ExecutionContext            = system.dispatcher
  var postgres: Option[EmbeddedPostgres]       = None
  var databaseService: Option[DatabaseService] = None

  override def beforeAll(): Unit = {
    postgres = Option(
      EmbeddedPostgres.builder
        .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
        .setCleanDataDirectory(true)
        .setPgBinaryResolver(new PostgresBinaryResolver)
        .start
    )

    val factory = new DatabaseServiceFactory()

    //DEOPSCSW-618: Create a method to locate a database server
    //DEOPSCSW-620: Create a method to make a connection to a database
    //DEOPSCSW-621: Create a session with a database
    databaseService = postgres.map(pg => factory.make(pg.getJdbcUrl("postgres", "postgres")))
  }

  override def afterAll(): Unit = {
    if (databaseService.isDefined) {
      databaseService.get.execute("DROP DATABASE box_office_scala;").futureValue(Timeout(Span(5, Seconds)))
      databaseService.get.execute("DROP TABLE budget_scala;").futureValue
      databaseService.get.execute("DROP TABLE films_scala;").futureValue
      databaseService.get.execute("DROP TABLE new_table_scala;").futureValue
      databaseService.get.closeConnection()
    }
    postgres.foreach(_.close)
    system.terminate().futureValue
  }

  private def withDatabaseService(block: DatabaseService => Assertion): Assertion = {
    if (databaseService.isDefined) {
      block(databaseService.get)
    } else {
      fail("could initialize database service")
    }
  }

  //DEOPSCSW-608: Examples of creating a database in the database service
  test("should be able to create a new Database") {
    withDatabaseService { databaseService: DatabaseService =>
      databaseService.execute("CREATE DATABASE box_office_scala;").futureValue(Timeout(Span(5, Seconds)))
      val resultSet = databaseService
        .executeQuery("""SELECT datname FROM pg_database WHERE datistemplate = false;""")
        .futureValue

      val databaseList = Iterator.from(0).takeWhile(_ => resultSet.next()).map(_ => resultSet.getString(1)).toList
      assert(databaseList contains "box_office_scala")
    }
  }

  //DEOPSCSW-609: Examples of creating records in a database in the database service
  //DEOPSCSW-613: Examples of querying records in a database in the database service
  test("should be able to create table with unique default IDs and insert data in it") {
    withDatabaseService { databaseService: DatabaseService =>
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
  }

  //DEOPSCSW-607: Complex relational database example
  //DEOPSCSW-609: Examples of creating records in a database in the database service
  //DEOPSCSW-613: Examples of querying records in a database in the database service
  test("should be able to create join and group records using ForeignKey") {
    withDatabaseService { databaseService: DatabaseService =>
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
  }

  //DEOPSCSW-611: Examples of updating records in a database in the database service
  //DEOPSCSW-619: Create a method to send an update sql string to a database
  test("should be able to update record") {
    withDatabaseService { databaseService: DatabaseService =>
      databaseService.execute("UPDATE films_scala SET name = 'movie_3' WHERE name = 'movie_2'").futureValue
      val resultSet =
        databaseService.executeQuery("SELECT count(*) AS rowCount from films_scala where name = 'movie_2';").futureValue
      resultSet.next()
      resultSet.getInt("rowCount") shouldBe 0
    }
  }

  //DEOPSCSW-612: Examples of deleting records in a database in the database service
  test("should be able to delete records") {
    withDatabaseService { databaseService: DatabaseService =>
      databaseService.execute("DELETE from films_scala WHERE name = 'movie_4'").futureValue
      val resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films_scala;").futureValue
      resultSet.next()
      resultSet.getInt("rowCount") shouldBe 2
    }
  }

  //DEOPSCSW-613: Examples of querying records in a database in the database service
  //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
  test("should be able to query records from the table") {
    withDatabaseService { databaseService: DatabaseService =>
      val resultSet = databaseService.executeQuery("SELECT * FROM films_scala where name = 'movie_1';").futureValue
      resultSet.next()
      resultSet.getString("name").trim shouldEqual "movie_1"
    }
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to drop a table") {
    withDatabaseService { databaseService: DatabaseService =>
      val dbm = databaseService.getConnectionMetaData.futureValue
      databaseService.execute("CREATE TABLE table_scala (id SERIAL PRIMARY KEY);").futureValue
      val tableCheckAfterCreate = dbm.getTables(null, null, "table_scala", null)
      assert(tableCheckAfterCreate.next)

      databaseService.execute("DROP TABLE table_scala;").futureValue
      val tableCheckAfterDrop = dbm.getTables(null, null, "table_scala", null)
      assert(!tableCheckAfterDrop.next)
    }
  }

  //DEOPSCSW-622: Modify a table using update sql string
  test("should be able to alter a table") {
    withDatabaseService { databaseService: DatabaseService =>
      databaseService.execute("CREATE TABLE new_table_scala (id SERIAL PRIMARY KEY);").futureValue
      val resultSetBeforeAlter = databaseService.executeQuery("SELECT * from new_table_scala;").futureValue
      val rsmd                 = resultSetBeforeAlter.getMetaData
      assert(rsmd.getColumnCount === 1)

      databaseService.execute("ALTER TABLE new_table_scala ADD COLUMN name VARCHAR(10);").futureValue
      val resultSetAfterAlter = databaseService.executeQuery("SELECT * from new_table_scala;").futureValue
      val rsmdAltered         = resultSetAfterAlter.getMetaData
      assert(rsmdAltered.getColumnCount === 2)
    }
  }
}
