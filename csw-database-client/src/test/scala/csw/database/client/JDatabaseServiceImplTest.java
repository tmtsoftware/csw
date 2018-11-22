package csw.database.client;

import akka.actor.ActorSystem;
import csw.database.api.javadasl.IDatabaseService;
import csw.database.client.scaladsl.DatabaseServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

//DEOPSCSW-601: Create Database API
public class JDatabaseServiceImplTest extends JUnitSuite {

    private static ActorSystem system;
    private static EmbeddedPostgres postgres;
    private static IDatabaseService databaseService;

    public JDatabaseServiceImplTest() throws IOException {
        system = ActorSystem.apply("test");
        ExecutionContext ec = system.dispatcher();
        postgres = new EmbeddedPostgres(Version.V10_6, "/tmp/postgresDataDir");
        int DEFAULT_PORT = 5435;

        final List<String> DEFAULT_ADD_PARAMS = asList("-E", "SQL_ASCII", "--locale=C", "--lc-collate=C", "--lc-ctype=C");

        postgres.start(
                EmbeddedPostgres.cachedRuntimeConfig(Paths.get("/tmp/postgresExtracted")),
                EmbeddedPostgres.DEFAULT_HOST,
                DEFAULT_PORT,
                EmbeddedPostgres.DEFAULT_DB_NAME,
                EmbeddedPostgres.DEFAULT_USER,
                EmbeddedPostgres.DEFAULT_PASSWORD,
                DEFAULT_ADD_PARAMS
        );
        //DEOPSCSW-618: Create a method to locate a database server
        //DEOPSCSW-620: Create a method to make a connection to a database
        //DEOPSCSW-621: Create a session with a database
        DatabaseServiceFactory factory = new DatabaseServiceFactory();
        databaseService = factory.jMake(postgres.getConnectionUrl().get(), ec);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        databaseService.execute("DROP DATABASE box_office_java;").get(5, SECONDS);
        databaseService.execute("DROP TABLE budget_java;").get(5, SECONDS);
        databaseService.execute("DROP TABLE films_java;").get(5, SECONDS);
        databaseService.execute("DROP TABLE new_table_java;").get(5, SECONDS);
        databaseService.closeConnection();
        postgres.stop();
        Runtime.getRuntime().exec("pkill postgres").waitFor();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    //DEOPSCSW-608: Examples of creating a database in the database service
    @Test
    public void shouldBeAbleToCreateANewDatabase() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE DATABASE box_office_java;").get(5, SECONDS);
        ResultSet resultSet =
                databaseService.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false;")
                        .get(5, SECONDS);

        List<String> databaseList = new ArrayList<>();
        while (resultSet.next()) databaseList.add(resultSet.getString(1));
        Assert.assertTrue(databaseList.contains("box_office_java"));
    }

    //DEOPSCSW-609: Examples of creating records in a database in the database service
    //DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateTableAndInsertDataInIt() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films_java (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL);").get(5, SECONDS);
        databaseService.execute("INSERT INTO films_java VALUES (DEFAULT, 'movie_1');").get(5, SECONDS);
        databaseService.execute("INSERT INTO films_java VALUES (DEFAULT, 'movie_2');").get(5, SECONDS);
        databaseService.execute("INSERT INTO films_java VALUES (DEFAULT, 'movie_3');").get(5, SECONDS);
        databaseService.execute("INSERT INTO films_java VALUES (DEFAULT, 'movie_4');").get(5, SECONDS);
        databaseService.execute("INSERT INTO films_java VALUES (DEFAULT, 'movie_5');").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films_java;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(5, resultSet.getInt("rowCount"));
    }

    //DEOPSCSW-607: Complex relational database example
    //DEOPSCSW-609: Examples of creating records in a database in the database service
    //DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateJoinAndGroupRecordsUsingForeignKey() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute(
                "CREATE TABLE budget_java (id SERIAL PRIMARY KEY," +
                        "movie_id INTEGER,movie_name VARCHAR(10)," +
                        "amount NUMERIC," +
                        "FOREIGN KEY (movie_id) REFERENCES films_java(id) " +
                        "ON DELETE CASCADE);"
        ).get(5, SECONDS);

        databaseService.execute("INSERT INTO budget_java VALUES (DEFAULT, 1, 'movie_1', 5000);").get(5, SECONDS);
        databaseService.execute("INSERT INTO budget_java VALUES (DEFAULT, 2, 'movie_2', 6000);").get(5, SECONDS);
        databaseService.execute("INSERT INTO budget_java VALUES (DEFAULT, 3, 'movie_3', 7000);").get(5, SECONDS);
        databaseService.execute("INSERT INTO budget_java VALUES (DEFAULT, 3, 'movie_3', 3000);").get(5, SECONDS);

        ResultSet resultSet =
                databaseService
                        .executeQuery(
                                "SELECT films_java.name, SUM(budget_java.amount) " +
                                        "FROM films_java INNER JOIN budget_java " +
                                        "ON films_java.id = budget_java.movie_id " +
                                        "GROUP BY films_java.name;"
                        )
                        .get(5, SECONDS);

        Set<String> databaseSet = new HashSet<>();
        while (resultSet.next()) databaseSet.add(resultSet.getString(1) + "," + resultSet.getInt(2));

        Assert.assertTrue(databaseSet.containsAll(Arrays.asList("movie_1,5000", "movie_3,10000", "movie_2,6000")));
    }

    //DEOPSCSW-611: Examples of updating records in a database in the database service
    //DEOPSCSW-619: Create a method to send an update sql string to a database
    @Test
    public void shouldBeAbleToUpdateRecordInTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("UPDATE films_java SET name = 'updated' WHERE name = 'movie_4'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films_java where name = 'movie_4';").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(0, resultSet.getInt("rowCount"));
    }

    //DEOPSCSW-612: Examples of deleting records in a database in the database service
    @Test
    public void shouldBeAbleToDeleteRecordsFromTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("DELETE from films_java WHERE name = 'movie_5'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films_java;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(4, resultSet.getInt("rowCount"));
    }

    //DEOPSCSW-613: Examples of querying records in a database in the database service
    //DEOPSCSW-616: Create a method to send a query (select) sql string to a database
    @Test
    public void shouldBeAbleToQueryRecordsFromTheTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        ResultSet resultSet = databaseService.executeQuery("SELECT * FROM films_java where name = 'movie_1';").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals("movie_1", resultSet.getString("name").trim());
    }

    //DEOPSCSW-622: Modify a table using update sql string
    @Test
    public void shouldBeAbleToDropATable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        DatabaseMetaData dbm = databaseService.getConnectionMetaData().get(5, SECONDS);
        databaseService.execute("CREATE TABLE table_java (id SERIAL PRIMARY KEY);").get(5, SECONDS);
        ResultSet tableCheckAfterCreate = dbm.getTables(null, null, "table_java", null);
        Assert.assertTrue(tableCheckAfterCreate.first());

        databaseService.execute("DROP TABLE table_java;").get(5, SECONDS);
        ResultSet tableCheckAfterDrop = dbm.getTables(null, null, "table_java", null);
        Assert.assertFalse(tableCheckAfterDrop.first());
    }

    //DEOPSCSW-622: Modify a table using update sql string
    @Test
    public void shouldBeAbleToAlterATable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE new_table_java (id SERIAL PRIMARY KEY);").get(5, SECONDS);
        ResultSet resultSetBeforeAlter = databaseService.executeQuery("SELECT * from new_table_java;").get(5, SECONDS);
        ResultSetMetaData rsmd = resultSetBeforeAlter.getMetaData();
        Assert.assertEquals(1, rsmd.getColumnCount());

        databaseService.execute("ALTER TABLE new_table_java ADD COLUMN name VARCHAR(10);").get(5, SECONDS);
        ResultSet resultSetAfterAlter = databaseService.executeQuery("SELECT * from new_table_java;").get(5, SECONDS);
        ResultSetMetaData rsmdAltered = resultSetAfterAlter.getMetaData();
        Assert.assertEquals(2, rsmdAltered.getColumnCount());
    }
}
