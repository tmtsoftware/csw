package csw.database;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.commons.DBTestHelper;
import csw.database.javadsl.JooqHelper;
import org.jooq.DSLContext;
import org.jooq.Queries;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.exception.DataAccessException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

//DEOPSCSW-601: Create Database API
//DEOPSCSW-616: Create a method to send a query (select) sql string to a database
public class JDatabaseServiceTest extends JUnitSuite {
    private static ActorSystem<SpawnProtocol.Command> system;
    private static EmbeddedPostgres postgres;
    private static DSLContext dsl;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.apply(SpawnProtocol.create(), "test");
        postgres = DBTestHelper.postgres(0); // 0 is random port
        dsl = DBTestHelper.dslContext(system, postgres.getPort());

    }

    @AfterClass
    public static void afterAll() throws Exception {
        postgres.close();
        system.terminate();
        Await.result(system.whenTerminated(), Duration.apply(5, SECONDS));
    }

    //DEOPSCSW-608: Examples of database creation
    @Test
    public void shouldBeAbleToCreateANewDatabase__DEOPSCSW_601_DEOPSCSW_616_DEOPSCSW_608() throws InterruptedException, ExecutionException, TimeoutException {
        String getDatabases = "SELECT datname FROM pg_database WHERE datistemplate = false";
        List<String> resultSet = JooqHelper.fetchAsync(dsl.resultQuery(getDatabases), String.class).get(5, SECONDS);

        // assert creation of database
        if (resultSet.contains("box_office")) {
            // drop box_office database
            dsl.query("DROP DATABASE box_office").executeAsync().toCompletableFuture().get(5, SECONDS);
            List<String> resultSet2 = JooqHelper.fetchAsync(dsl.resultQuery(getDatabases), String.class).get(5, SECONDS);
            assertFalse(resultSet2.contains("box_office"));
        }


        // create box_office database
        dsl.query("CREATE DATABASE box_office").executeAsync().toCompletableFuture().get(5, SECONDS);
        List<String> resultSet3 = JooqHelper.fetchAsync(dsl.resultQuery(getDatabases), String.class).get(5, SECONDS);

        // assert creation of database
        assertTrue(resultSet3.contains("box_office"));

        // drop box_office database
        dsl.query("DROP DATABASE box_office").executeAsync().toCompletableFuture().get(5, SECONDS);
        List<String> resultSet4 = JooqHelper.fetchAsync(dsl.resultQuery(getDatabases), String.class).get(5, SECONDS);
        assertFalse(resultSet4.contains("box_office"));
    }

    //DEOPSCSW-622: Modify a table using update sql string
    @Test
    public void shouldBeAbleToAlterOrDropATable__DEOPSCSW_601_DEOPSCSW_616_DEOPSCSW_622() throws InterruptedException, ExecutionException, TimeoutException {
        // create films
        dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY)").executeAsync().toCompletableFuture().get(5, SECONDS);

        ResultQuery<Record> getTables = dsl.resultQuery("select table_name from information_schema.tables");
        List<String> tableResultSet = JooqHelper.fetchAsync(getTables, String.class).get(5, SECONDS);
        assertTrue(tableResultSet.contains("films"));

        String getColumnCount = "SELECT Count(*) FROM INFORMATION_SCHEMA.Columns where TABLE_NAME = 'films'";
        List<Integer> resultSetBeforeAlter = JooqHelper.fetchAsync(dsl.resultQuery(getColumnCount), Integer.class).get(5, SECONDS);
        assertEquals(Integer.valueOf(1), resultSetBeforeAlter.get(0));

        // add one more column in films
        dsl.query("ALTER TABLE films ADD COLUMN name VARCHAR(10)").executeAsync().toCompletableFuture().get(5, SECONDS);

        // assert increased count of column in films
        List<Integer> resultSetAfterAlter = JooqHelper.fetchAsync(dsl.resultQuery(getColumnCount), Integer.class).get(5, SECONDS);
        assertEquals(Integer.valueOf(2), resultSetAfterAlter.get(0));

        // drop table
        dsl.query("DROP TABLE films").executeAsync().toCompletableFuture().get(5, SECONDS);

        // assert removal of table
        List<String> tableResultSet2 = JooqHelper.fetchAsync(getTables, String.class).get(5, SECONDS);
        assertFalse(tableResultSet2.contains("films"));
    }

    //DEOPSCSW-613: Examples of querying records
    //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
    //DEOPSCSW-610: Examples of Reading Records
    //DEOPSCSW-609: Examples of Record creation
    @Test
    public void shouldBeAbleToQueryRecordsFromTheTable__DEOPSCSW_613_DEOPSCSW_610_DEOPSCSW_601_DEOPSCSW_609_DEOPSCSW_616() throws InterruptedException, ExecutionException, TimeoutException {
        // create films and insert movie_1
        String movieName = "movie_1";
        String movieName2 = "movie_2";
        dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").executeAsync().toCompletableFuture().get(5, SECONDS);
        dsl.query("INSERT INTO films(name) VALUES (?)", movieName).executeAsync().toCompletableFuture().get(5, SECONDS);
        dsl.query("INSERT INTO films(name) VALUES (?)", movieName2).executeAsync().toCompletableFuture().get(5, SECONDS);

        // query the table and assert on data received
        List<Film> resultSet = JooqHelper.fetchAsync(dsl.resultQuery("SELECT * FROM films where name = ?", movieName), Film.class).get(5, SECONDS);
        assertEquals(resultSet, List.of(new Film(1, movieName)));

        dsl.query("DROP TABLE films").executeAsync().toCompletableFuture().get(5, SECONDS);
    }

    //DEOPSCSW-607: Complex relational database example
    //DEOPSCSW-609: Examples of Record creation
    //DEOPSCSW-613: Examples of querying records
    //DEOPSCSW-610: Examples of Reading Records
    @Test
    public void shouldBeAbleToCreateJoinAndGroupRecordsUsingForeignKey__DEOPSCSW_607_DEOPSCSW_613_DEOPSCSW_610_DEOPSCSW_601_DEOPSCSW_609_DEOPSCSW_616() throws InterruptedException, ExecutionException, TimeoutException {
        // create tables films and budget and insert records
        Queries queries = dsl.queries(
                dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)"),
                dsl.query("INSERT INTO films(name) VALUES ('movie_1')"),
                dsl.query("INSERT INTO films(name) VALUES ('movie_4')"),
                dsl.query("INSERT INTO films(name) VALUES ('movie_2')"),
                dsl.query("CREATE TABLE budget (id SERIAL PRIMARY KEY," +
                        "movie_id INTEGER,movie_name VARCHAR(10)," +
                        "amount NUMERIC," +
                        "FOREIGN KEY (movie_id) REFERENCES films(id) " +
                        "ON DELETE CASCADE);"),
                dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (1, 'movie_1', 5000)"),
                dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (2, 'movie_4', 6000)"),
                dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 7000)"),
                dsl.query("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 3000)")
        );
        JooqHelper.executeBatch(queries).get(5, SECONDS);

        // query with joins and group by
        List<FilmBudget> resultSet = JooqHelper
                .fetchAsync(dsl.resultQuery(
                                "SELECT films.name, SUM(budget.amount) " +
                                        "FROM films INNER JOIN budget " +
                                        "ON films.id = budget.movie_id " +
                                        "GROUP BY films.name;"),
                        FilmBudget.class)
                .get(5, SECONDS);

        List<FilmBudget> expectedResult = Arrays.asList(
                new FilmBudget("movie_1", 5000),
                new FilmBudget("movie_2", 10000),
                new FilmBudget("movie_4", 6000)
        );

        assertTrue(resultSet.containsAll(expectedResult));

        dsl.query("DROP TABLE budget").executeAsync().toCompletableFuture().get(5, SECONDS);
        dsl.query("DROP TABLE films").executeAsync().toCompletableFuture().get(5, SECONDS);
    }

    //DEOPSCSW-611: Examples of updating records
    //DEOPSCSW-619: Create a method to send an update sql string to a database
    @Test
    public void shouldBeAbleToUpdateRecord__DEOPSCSW_601_DEOPSCSW_616_DEOPSCSW_611_DEOPSCSW_619() throws InterruptedException, ExecutionException, TimeoutException {
        // create films and insert record
        String movie_2 = "movie_2";
        Queries queries = dsl.queries(
                dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)"),
                dsl.query("INSERT INTO films(name) VALUES (?)", movie_2)
        );

        JooqHelper.executeBatch(queries).get(5, SECONDS);

        // update record value
        dsl.query("UPDATE films SET name = 'movie_3' WHERE name = ?", movie_2).executeAsync().toCompletableFuture().get(5, SECONDS);

        // assert the record is updated
        List<Integer> resultSet = JooqHelper.fetchAsync(dsl.resultQuery("SELECT count(*) AS rowCount from films where name = ?", movie_2), Integer.class).get(5, SECONDS);
        assertEquals(Integer.valueOf(0), resultSet.get(0));

        dsl.query("DROP TABLE films").executeAsync().toCompletableFuture().get(5, SECONDS);
    }

    //DEOPSCSW-612: Examples of deleting records
    @Test
    public void shouldBeAbleToDeleteRecordsFromTable__DEOPSCSW_601_DEOPSCSW_616_DEOPSCSW_612() throws InterruptedException, ExecutionException, TimeoutException {
        String movie4 = "movie_4";
        Queries queries = dsl.queries(
                dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)"),
                dsl.query("INSERT INTO films(name) VALUES ('movie_1')"),
                dsl.query("INSERT INTO films(name) VALUES (?)", movie4),
                dsl.query("INSERT INTO films(name) VALUES ('movie_2')")
        );

        JooqHelper.executeBatch(queries).get(5, SECONDS);

        // assert the entry of record
        List<String> resultSet = JooqHelper.fetchAsync(dsl.resultQuery("SELECT name from films where name=?", movie4), String.class).get(5, SECONDS);
        assertEquals(resultSet, List.of(movie4));

        // delete movie_4
        dsl.query("DELETE from films WHERE name = ?", movie4).executeAsync().toCompletableFuture().get(5, SECONDS);

        // assert the removal of record
        List<String> resultSet2 = JooqHelper.fetchAsync(dsl.resultQuery("SELECT name from films where name=?", movie4), String.class).get(5, SECONDS);
        assertEquals(resultSet2, Collections.emptyList());

        dsl.query("DROP TABLE films").executeAsync().toCompletableFuture().get(5, SECONDS);
    }

    @Test
    public void shouldBeThrowingExceptionInCaseOfSyntaxError__DEOPSCSW_601_DEOPSCSW_616() throws InterruptedException, ExecutionException {
        ExecutionException ex = Assert.assertThrows(ExecutionException.class, () ->
                dsl.query("create1 table tableName (id SERIAL PRIMARY KEY)").executeAsync().toCompletableFuture().get()
        );
        Assert.assertTrue(ex.getCause() instanceof DataAccessException);
    }

    @Test
    public void shouldBeAbleToCreateAFunctionAndQueryIt__DEOPSCSW_601_DEOPSCSW_616() throws InterruptedException, ExecutionException, TimeoutException {
        dsl.query("CREATE FUNCTION inc(val integer) RETURNS integer AS $$\n" +
                "BEGIN\n" +
                "RETURN val + 1;\n" +
                "END; $$\n" +
                "LANGUAGE PLPGSQL;").executeAsync().toCompletableFuture().get(5, SECONDS);

        List<Integer> resultSet = JooqHelper.fetchAsync(dsl.resultQuery("select inc(20)"), Integer.class).get(5, SECONDS);

        assertEquals(List.of(21), resultSet);
    }
}

class Film {
    private final Integer id;
    private final String name;

    Film(Integer id, String name) {
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
    private final String name;
    private final Integer amt;

    FilmBudget(String name, Integer amt) {
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
