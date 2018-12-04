package csw.database.client;

import akka.actor.ActorSystem;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.api.javadsl.IDatabaseService;
import csw.database.api.models.DBRow;
import csw.database.client.scaladsl.DatabaseServiceFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

//DEOPSCSW-601: Create Database API
public class JDatabaseServiceTest extends JUnitSuite {

    private static ActorSystem system;
    private static EmbeddedPostgres postgres;
    private static IDatabaseService databaseService;

    @BeforeClass
    public static void setup() throws IOException {
        postgres = EmbeddedPostgres
                .builder()
                .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
                .setCleanDataDirectory(true)
                .setPgBinaryResolver(new PostgresBinaryResolver())
                .start();

        system = ActorSystem.apply("test");
        ExecutionContext ec = system.dispatcher();

        //DEOPSCSW-620: Create a method to make a connection to a database
        DatabaseServiceFactory factory = new DatabaseServiceFactory();
        databaseService = factory.jMake("localhost", postgres.getPort(), "postgres", "postgres", ec);

    }

    @AfterClass
    public static void afterAll() throws Exception {
        postgres.close();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    //DEOPSCSW-608: Examples of creating a database in the database service
    @Test
    public void shouldBeAbleToCreateANewDatabase() throws InterruptedException, ExecutionException, TimeoutException {
        databaseService.update("CREATE DATABASE box_office").get(5, SECONDS);
        String getDatabases = "SELECT datname FROM pg_database WHERE datistemplate = false";
        List<String> resultSet = databaseService.select(getDatabases, DBRow::nextString).get(5, SECONDS);
        assertTrue(resultSet.contains("box_office"));

        databaseService.update("DROP DATABASE box_office").get(5, SECONDS);
        List<String> resultSet2 = databaseService.select(getDatabases, DBRow::nextString).get(5, SECONDS);
        assertFalse(resultSet2.contains("box_office"));
    }

    //DEOPSCSW-622: Modify a table using update sql string
    @Test
    public void shouldBeAbleToAlterOrDropATable() throws InterruptedException, ExecutionException, TimeoutException {
        databaseService.update("CREATE TABLE films (id SERIAL PRIMARY KEY)").get(5, SECONDS);
        String getColumnCount = "SELECT Count(*) FROM INFORMATION_SCHEMA.Columns where TABLE_NAME = 'films'";
        List<Integer> resultSetBeforeAlter = databaseService.select(getColumnCount, DBRow::nextInt).get(5, SECONDS);
        assertEquals(Integer.valueOf(1), resultSetBeforeAlter.get(0));

        databaseService.update("ALTER TABLE films ADD COLUMN name VARCHAR(10)").get(5, SECONDS);
        List<Integer> resultSetAfterAlter = databaseService.select(getColumnCount, DBRow::nextInt).get(5, SECONDS);
        assertEquals(Integer.valueOf(2), resultSetAfterAlter.get(0));

        String getTables = "select table_name from information_schema.tables";
        List<String> tableResultSet = databaseService.select(getTables, DBRow::nextString).get(5, SECONDS);
        assertTrue(tableResultSet.contains("films"));

        databaseService.update("DROP TABLE films").get(5, SECONDS);

        List<String> tableResultSet2 = databaseService.select(getTables, DBRow::nextString).get(5, SECONDS);
        assertFalse(tableResultSet2.contains("films"));
    }

    //DEOPSCSW-613: Examples of querying records
    //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
    //DEOPSCSW-610: Examples of Reading Records
    @Test
    public void shouldBeAbleToQueryRecordsFromTheTable() throws InterruptedException, ExecutionException, TimeoutException {
        List<String> queries = new ArrayList<>();
        queries.add("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)");
        queries.add("INSERT INTO films(name) VALUES ('movie_1')");

        // create films db
        databaseService.updateAll(queries).get(5, SECONDS);

        List<Film> resultSet =
                databaseService.select(
                        "SELECT * FROM films where name = 'movie_1'",
                        r -> new Film(r.nextInt(), r.nextString())
                ).get(5, SECONDS);

        assertTrue(resultSet.contains(new Film(1, "movie_1")));

        databaseService.update("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-609: Examples of creating records in a database in the database service
    //DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateTableAndInsertDataInIt() throws InterruptedException, ExecutionException, TimeoutException {
        List<String> queries = new ArrayList<>();
        queries.add("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)");
        queries.add("INSERT INTO films(name) VALUES ('movie_1')");
        queries.add("INSERT INTO films(name) VALUES ('movie_4')");
        queries.add("INSERT INTO films(name) VALUES ('movie_2')");

        databaseService.updateAll(queries).get(5, SECONDS);

        List<Integer> resultSet = databaseService.select("SELECT count(*) AS rowCount from films", DBRow::nextInt).get(5, SECONDS);
        assertEquals(Integer.valueOf(3), resultSet.get(0));

        databaseService.update("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-607: Complex relational database example
    //DEOPSCSW-609: Examples of creating records in a database in the database service
    //DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateJoinAndGroupRecordsUsingForeignKey() throws InterruptedException, ExecutionException, TimeoutException {
        List<String> queries = new ArrayList<>();
        queries.add("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)");
        queries.add("INSERT INTO films(name) VALUES ('movie_1')");
        queries.add("INSERT INTO films(name) VALUES ('movie_4')");
        queries.add("INSERT INTO films(name) VALUES ('movie_2')");
        queries.add(
                "CREATE TABLE budget (id SERIAL PRIMARY KEY," +
                        "movie_id INTEGER,movie_name VARCHAR(10)," +
                        "amount NUMERIC," +
                        "FOREIGN KEY (movie_id) REFERENCES films(id) ON DELETE CASCADE)"
        );
        queries.add("INSERT INTO budget(movie_id, movie_name, amount) VALUES (1, 'movie_1', 5000)");
        queries.add("INSERT INTO budget(movie_id, movie_name, amount) VALUES (2, 'movie_4', 6000)");
        queries.add("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 7000)");
        queries.add("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 3000)");

        databaseService.updateAll(queries).get(5, SECONDS);

        List<FilmBudget> resultSet = databaseService
                .select(
                        "SELECT films.name, SUM(budget.amount) " +
                                "FROM films INNER JOIN budget " +
                                "ON films.id = budget.movie_id " +
                                "GROUP BY films.name;",
                        r -> new FilmBudget(r.nextString(), r.nextInt())
                )
                .get(5, SECONDS);

        List<FilmBudget> expectedResult = Arrays.asList(
                new FilmBudget("movie_1", 5000),
                new FilmBudget("movie_2", 10000),
                new FilmBudget("movie_4", 6000)
        );

        assertTrue(resultSet.containsAll(expectedResult));

        databaseService.update("DROP TABLE budget").get(5, SECONDS);
        databaseService.update("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-611: Examples of updating records
    //DEOPSCSW-619: Create a method to send an update sql string to a database
    @Test
    public void shouldBeAbleToUpdateRecord() throws InterruptedException, ExecutionException, TimeoutException {
        List<String> queries = new ArrayList<>();
        queries.add("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)");
        queries.add("INSERT INTO films(name) VALUES ('movie_2')");
        queries.add("UPDATE films SET name = 'movie_3' WHERE name = 'movie_2'");

        databaseService.updateAll(queries).get(5, SECONDS);

        List<Integer> resultSet = databaseService.select("SELECT count(*) AS rowCount from films where name = 'movie_4'", DBRow::nextInt).get(5, SECONDS);
        assertEquals(Integer.valueOf(0), resultSet.get(0));

        databaseService.update("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-612: Examples of deleting records
    @Test
    public void shouldBeAbleToDeleteRecordsFromTable() throws InterruptedException, ExecutionException, TimeoutException {
        List<String> queries = new ArrayList<>();
        queries.add("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)");
        queries.add("INSERT INTO films(name) VALUES ('movie_1')");
        queries.add("INSERT INTO films(name) VALUES ('movie_4')");
        queries.add("INSERT INTO films(name) VALUES ('movie_2')");
        queries.add("DELETE from films WHERE name = 'movie_4'");

        databaseService.updateAll(queries).get(5, SECONDS);
        List<Integer> resultSet = databaseService.select("SELECT count(*) AS rowCount from films", DBRow::nextInt).get(5, SECONDS);
        assertEquals(Integer.valueOf(2), resultSet.get(0));

        databaseService.update("DROP TABLE films").get(5, SECONDS);
    }
}

class Film {
    private Integer id;
    private String name;

    public Film(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object operand) {
        if (operand == this) {
            return true;
        }
        if (!(operand instanceof Film)) {
            return false;
        }
        Film current = (Film) operand;
        return name.equals(current.name) && id.equals(current.id);
    }
}

class FilmBudget {
    private String name;
    private Integer amt;

    public FilmBudget(String name, Integer amt) {
        this.name = name;
        this.amt = amt;
    }

    @Override
    public boolean equals(Object operand) {
        if (operand == this) {
            return true;
        }
        if (!(operand instanceof FilmBudget)) {
            return false;
        }
        FilmBudget current = (FilmBudget) operand;
        return name.equals(current.name) && amt.equals(current.amt);
    }
}