package csw.database;

import akka.actor.ActorSystem;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.commons.DBTestHelper;
import csw.database.exceptions.DatabaseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.isA;

//DEOPSCSW-615: DB service accessible to CSW component developers
public class JDatabaseServiceFactoryFailureTest extends JUnitSuite {

    private static ActorSystem system;
    private static EmbeddedPostgres postgres;
    private static DatabaseServiceFactory dbFactory;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.apply("test");
        dbFactory = DBTestHelper.dbServiceFactory(system);
        postgres = DBTestHelper.postgres(0); // 0 is random port
    }

    @AfterClass
    public static void afterAll() throws Exception {
        postgres.close();
        Await.result(system.terminate(), Duration.apply(5, SECONDS));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldThrowDatabaseConnectionWhileConnectingWithIncorrectPort() throws InterruptedException, ExecutionException {
        exception.expectCause(isA(DatabaseException.class));
        dbFactory.jMakeDsl().get();
    }
}
