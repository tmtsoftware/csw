package csw.database.client;

import akka.actor.ActorSystem;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.api.javadasl.IDatabaseService;
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
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
        databaseService = factory.jMake(postgres.getJdbcUrl("postgres", "postgres"), ec);

    }

    @AfterClass
    public static void afterAll() throws Exception {
        databaseService.closeConnection();
        postgres.close();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    //DEOPSCSW-608: Examples of creating a database in the database service
    @Test
    public void shouldBeAbleToCreateANewDatabase() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE DATABASE box_office").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false").get(5, SECONDS);

        List<String> databaseList = new ArrayList<>();

        while (resultSet.next()) databaseList.add(resultSet.getString(1));
        assertTrue(databaseList.contains("box_office"));

        databaseService.execute("DROP DATABASE box_office").get(5, SECONDS);
        ResultSet resultSet2 = databaseService.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false").get(5, SECONDS);

        databaseList.clear();

        while (resultSet2.next()) databaseList.add(resultSet2.getString(1));

        assertFalse(databaseList.contains("box_office"));
    }

    //DEOPSCSW-622: Modify a table using update sql string
    @Test
    public void shouldBeAbleToAlterOrDropATable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY)").get(5, SECONDS);
        ResultSet resultSetBeforeAlter = databaseService.executeQuery("SELECT * from films").get(5, SECONDS);
        ResultSetMetaData rsmd = resultSetBeforeAlter.getMetaData();
        assertEquals(1, rsmd.getColumnCount());

        databaseService.execute("ALTER TABLE films ADD COLUMN name VARCHAR(10)").get(5, SECONDS);
        ResultSet resultSetAfterAlter = databaseService.executeQuery("SELECT * from films").get(5, SECONDS);
        ResultSetMetaData rsmdAltered = resultSetAfterAlter.getMetaData();
        assertEquals(2, rsmdAltered.getColumnCount());

        List<String> tables = new ArrayList<>();
        ResultSet tableResultSet = databaseService.executeQuery("select table_name from information_schema.tables").get(5, SECONDS);
        while (tableResultSet.next()) tables.add(tableResultSet.getString(1));

        assertTrue(tables.contains("films"));

        databaseService.execute("DROP TABLE films").get(5, SECONDS);

        tables.clear();
        ResultSet tableResultSet2 = databaseService.executeQuery("select table_name from information_schema.tables").get(5, SECONDS);
        while (tableResultSet2.next()) tables.add(tableResultSet2.getString(1));

        assertFalse(tables.contains("films"));
    }

    //DEOPSCSW-613: Examples of querying records
    //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
    //DEOPSCSW-610: Examples of Reading Records
    @Test
    public void shouldBeAbleToQueryRecordsFromTheTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        // create films db
        databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery("SELECT * FROM films where name = 'movie_1'").get(5, SECONDS);
        resultSet.next();
        assertEquals("movie_1", resultSet.getString("name").trim());

        databaseService.execute("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-609: Examples of creating records in a database in the database service
    //DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateTableAndInsertDataInIt() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_4')").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films").get(5, SECONDS);
        resultSet.next();
        assertEquals(3, resultSet.getInt("rowCount"));

        databaseService.execute("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-607: Complex relational database example
    //DEOPSCSW-609: Examples of creating records in a database in the database service
    //DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateJoinAndGroupRecordsUsingForeignKey() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_4')").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").get(5, SECONDS);

        databaseService
                .execute(
                        "CREATE TABLE budget (id SERIAL PRIMARY KEY," +
                                "movie_id INTEGER,movie_name VARCHAR(10)," +
                                "amount NUMERIC," +
                                "FOREIGN KEY (movie_id) REFERENCES films(id) " +
                                "ON DELETE CASCADE);"
                )
                .get(5, SECONDS);

        databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (1, 'movie_1', 5000)").get(5, SECONDS);
        databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (2, 'movie_4', 6000)").get(5, SECONDS);
        databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 7000)").get(5, SECONDS);
        databaseService.execute("INSERT INTO budget(movie_id, movie_name, amount) VALUES (3, 'movie_2', 3000)").get(5, SECONDS);

        ResultSet resultSet = databaseService
                .executeQuery(
                        "SELECT films.name, SUM(budget.amount) " +
                                "FROM films INNER JOIN budget " +
                                "ON films.id = budget.movie_id " +
                                "GROUP BY films.name;"
                )
                .get(5, SECONDS);

        Set<String> databaseSet = new HashSet<>();
        while (resultSet.next()) databaseSet.add(resultSet.getString(1) + "," + resultSet.getInt(2));

        assertTrue(databaseSet.containsAll(Arrays.asList("movie_1,5000", "movie_2,10000", "movie_4,6000")));

        databaseService.execute("DROP TABLE budget").get(5, SECONDS);
        databaseService.execute("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-611: Examples of updating records
    //DEOPSCSW-619: Create a method to send an update sql string to a database
    @Test
    public void shouldBeAbleToUpdateRecord() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").get(5, SECONDS);


        databaseService.execute("UPDATE films SET name = 'movie_3' WHERE name = 'movie_2'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films where name = 'movie_4'").get(5, SECONDS);
        resultSet.next();
        assertEquals(0, resultSet.getInt("rowCount"));

        databaseService.execute("DROP TABLE films").get(5, SECONDS);
    }

    //DEOPSCSW-612: Examples of deleting records
    @Test
    public void shouldBeAbleToDeleteRecordsFromTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL)").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_1')").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_4')").get(5, SECONDS);
        databaseService.execute("INSERT INTO films(name) VALUES ('movie_2')").get(5, SECONDS);

        databaseService.execute("DELETE from films WHERE name = 'movie_4'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films").get(5, SECONDS);
        resultSet.next();
        assertEquals(2, resultSet.getInt("rowCount"));

        databaseService.execute("DROP TABLE films").get(5, SECONDS);
    }
}
