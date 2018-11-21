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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

// DEOPSCSW-601: Create Database API
public class JDatabaseServiceImplTest extends JUnitSuite {

    private static ActorSystem system;
    private static EmbeddedPostgres postgres;
    private static IDatabaseService databaseService;

    public JDatabaseServiceImplTest() throws IOException {
        int port = 5434;
        String host = "localhost";
        String dbName = "test";
        system = ActorSystem.apply("test");
        ExecutionContext ec = system.dispatcher();
        postgres = new EmbeddedPostgres(Version.V10_6);
        postgres.start(host, port, dbName);

        // DEOPSCSW-618: Create a method to locate a database server
        // DEOPSCSW-620: Create a method to make a connection to a database
        // DEOPSCSW-621: Create a session with a database
        DatabaseServiceFactory factory = new DatabaseServiceFactory();
        databaseService = factory.jMake(host, port, dbName, ec);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        databaseService.execute("DROP DATABASE Jbox_office;").get(5, SECONDS);
        databaseService.execute("DROP TABLE Jbudget;").get(5, SECONDS);
        databaseService.execute("DROP TABLE Jfilms;").get(5, SECONDS);
        postgres.stop();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    // DEOPSCSW-608: Examples of creating a database in the database service
    @Test
    public void shouldBeAbleToCreateANewDatabase() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE DATABASE Jbox_office;").get(5, SECONDS);
        ResultSet resultSet =
                databaseService.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false;")
                        .get(5, SECONDS);

        ArrayList<String> databaseList = new ArrayList<String>();
        while (resultSet.next()) databaseList.add(resultSet.getString(1));
        Assert.assertEquals("[postgres, test, jbox_office]", databaseList.toString());
    }

    // DEOPSCSW-609: Examples of creating records in a database in the database service
    // DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateTableAndInsertDataInIt() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE Jfilms (id SERIAL PRIMARY KEY, name VARCHAR (10) UNIQUE NOT NULL);").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES (DEFAULT, 'movie_1');").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES (DEFAULT, 'movie_2');").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES (DEFAULT, 'movie_3');").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES (DEFAULT, 'movie_4');").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES (DEFAULT, 'movie_5');").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from Jfilms;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(5, resultSet.getInt("rowCount"));
    }

    // DEOPSCSW-607: Complex relational database example
    // DEOPSCSW-609: Examples of creating records in a database in the database service
    // DEOPSCSW-613: Examples of querying records in a database in the database service
    @Test
    public void shouldBeAbleToCreateJoinAndGroupRecordsUsingForeignKey() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute(
                "CREATE TABLE Jbudget (id SERIAL PRIMARY KEY," +
                        "movie_id INTEGER,movie_name VARCHAR(10)," +
                        "amount NUMERIC," +
                        "FOREIGN KEY (movie_id) REFERENCES Jfilms(id) " +
                        "ON DELETE CASCADE);"
        ).get(5, SECONDS);
        databaseService.execute("INSERT INTO Jbudget VALUES (DEFAULT, 1, 'movie_1', 5000);").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jbudget VALUES (DEFAULT, 2, 'movie_2', 6000);").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jbudget VALUES (DEFAULT, 3, 'movie_3', 7000);").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jbudget VALUES (DEFAULT, 3, 'movie_3', 3000);").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery(
                "SELECT Jfilms.name, SUM(Jbudget.amount) " +
                        "FROM Jfilms INNER JOIN Jbudget " +
                        "ON Jfilms.id = Jbudget.movie_id " +
                        "GROUP BY Jfilms.name;"
        ).get(5, SECONDS);

        ArrayList<String> databaseList = new ArrayList<String>();
        while (resultSet.next()) databaseList.add(resultSet.getString(1) + "," + resultSet.getInt(2));
        Assert.assertEquals("[movie_1,5000, movie_3,10000, movie_2,6000]", databaseList.toString());
    }

    // DEOPSCSW-611: Examples of updating records in a database in the database service
    // DEOPSCSW-619: Create a method to send an update sql string to a database
    @Test
    public void shouldBeAbleToUpdateRecordInTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("UPDATE Jfilms SET name = 'updated' WHERE name = 'movie_4'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from Jfilms where name = 'movie_4';").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(0, resultSet.getInt("rowCount"));
    }

    // DEOPSCSW-612: Examples of deleting records in a database in the database service
    @Test
    public void shouldBeAbleToDeleteRecordsFromTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("DELETE from Jfilms WHERE name = 'movie_5'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from Jfilms;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(4, resultSet.getInt("rowCount"));
    }

    // DEOPSCSW-613: Examples of querying records in a database in the database service
    // DEOPSCSW-616: Create a method to send a query (select) sql string to a database
    @Test
    public void shouldBeAbleToQueryRecordsFromTheTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        ResultSet resultSet = databaseService.executeQuery("SELECT * FROM Jfilms where name = 'movie_1';").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals("movie_1", resultSet.getString("name").trim());
    }
}
