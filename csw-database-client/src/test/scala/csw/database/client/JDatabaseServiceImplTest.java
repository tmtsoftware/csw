package csw.database.client;

import akka.actor.ActorSystem;
import csw.database.api.javadasl.IDatabaseService;
import csw.database.client.scaladsl.DatabaseServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
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

public class JDatabaseServiceImplTest {

    private static ActorSystem system;
    private static EmbeddedPostgres postgres;
    private IDatabaseService databaseService;

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
        postgres.stop();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    @Test
    public void shouldBeAbleToCreateTableAndInsertDataInIt() throws InterruptedException, ExecutionException, TimeoutException, SQLException {
        databaseService.execute("CREATE TABLE films (code char(10));").get(5, SECONDS);
        databaseService.execute("INSERT INTO films VALUES ('movie_1');").get(5, SECONDS);
        databaseService.execute("INSERT INTO films VALUES ('movie_4');").get(5, SECONDS);
        databaseService.execute("INSERT INTO films VALUES ('movie_2');").get(5, SECONDS);

        ResultSet resultSet = databaseService.executeQuery("SELECT count(*) AS rowCount from films;").get(5, SECONDS);
        resultSet.next();
        Assert.assertEquals(3, resultSet.getInt("rowCount"));
    }
}