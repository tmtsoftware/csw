package csw.database;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.commons.DBTestHelper;
import csw.database.exceptions.DatabaseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;

//DEOPSCSW-615: DB service accessible to CSW component developers
public class JDatabaseServiceFactoryFailureTest extends JUnitSuite {

    private static ActorSystem<SpawnProtocol.Command> system;
    private static EmbeddedPostgres postgres;
    private static DatabaseServiceFactory dbFactory;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.apply(SpawnProtocol.create(), "test");
        dbFactory = DBTestHelper.dbServiceFactory(system);
        postgres = DBTestHelper.postgres(0); // 0 is random port
    }

    @AfterClass
    public static void afterAll() throws Exception {
        postgres.close();
        system.terminate();
        Await.result(system.whenTerminated(), Duration.apply(5, SECONDS));
    }

    @Test
    public void shouldThrowDatabaseConnectionWhileConnectingWithIncorrectPort__DEOPSCSW_615() {
        ExecutionException ex = Assert.assertThrows(ExecutionException.class, () -> dbFactory.jMakeDsl().get());
        Assert.assertTrue(ex.getCause() instanceof DatabaseException);
    }
}
