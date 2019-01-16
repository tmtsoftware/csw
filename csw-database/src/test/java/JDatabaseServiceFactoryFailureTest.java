import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.DatabaseServiceFactory;
import csw.database.commons.DatabaseServiceConnection;
import csw.database.exceptions.DatabaseException;
import csw.database.javadsl.JooqHelper;
import csw.database.commons.DBTestHelper;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.TcpRegistration;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.server.http.JHTTPLocationService;
import org.hamcrest.CoreMatchers;
import org.jooq.DSLContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static csw.database.DatabaseServiceFactory.ReadPasswordHolder;
import static csw.database.DatabaseServiceFactory.ReadUsernameHolder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//DEOPSCSW-615: DB service accessible to CSW component developers
public class JDatabaseServiceFactoryFailureTest extends JUnitSuite {

    private static ActorSystem system;
    private static EmbeddedPostgres postgres;
    private static DatabaseServiceFactory dbFactory;

    @BeforeClass
    public static void setup() throws ExecutionException, InterruptedException, TimeoutException {
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
