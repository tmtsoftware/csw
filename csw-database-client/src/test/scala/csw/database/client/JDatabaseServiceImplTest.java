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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

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
        DatabaseServiceFactory factory = new DatabaseServiceFactory();
        databaseService = factory.jMake(host, port, dbName, ec);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        databaseService.execute("DROP TABLE Jfilms;").get(5, SECONDS);
        postgres.stop();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    @Test
    public void shouldBeAbleToCreateTableAndInsertDataInIt() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE Jfilms (code char(10));").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES ('movie_1');").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES ('movie_4');").get(5, SECONDS);
        databaseService.execute("INSERT INTO Jfilms VALUES ('movie_2');").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from Jfilms;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(3, resultSet.getInt("rowCount"));
    }

    @Test
    public void shouldBeAbleToUpdateRecordInTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("UPDATE Jfilms SET code = 'movie_3' WHERE code = 'movie_2'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from Jfilms where code = 'movie_2';").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(0, resultSet.getInt("rowCount"));
    }

    @Test
    public void shouldBeAbleToDeleteRecordsFromTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("DELETE from Jfilms WHERE code = 'movie_4'").get(5, SECONDS);
        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from Jfilms;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(2, resultSet.getInt("rowCount"));
    }

    @Test
    public void shouldBeAbleToQueryRecordsFromTheTable() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        ResultSet resultSet = databaseService.executeQuery("SELECT * FROM Jfilms where code = 'movie_1';").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals("movie_1", resultSet.getString("code").trim());
    }
}