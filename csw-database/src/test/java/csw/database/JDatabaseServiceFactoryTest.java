package csw.database;

import akka.actor.typed.SpawnProtocol;
import akka.stream.Materializer;
import akka.stream.typed.javadsl.ActorMaterializerFactory;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import csw.database.commons.DBTestHelper;
import csw.database.commons.DatabaseServiceConnection;
import csw.database.javadsl.JooqHelper;
import csw.location.api.javadsl.ILocationService;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.models.TcpRegistration;
import csw.location.server.http.JHTTPLocationService;
import org.jooq.DSLContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static csw.database.DatabaseServiceFactory.ReadPasswordHolder;
import static csw.database.DatabaseServiceFactory.ReadUsernameHolder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//DEOPSCSW-620: Session Creation to access data
//DEOPSCSW-621: Session creation to access data with single connection
//DEOPSCSW-615: DB service accessible to CSW component developers
public class JDatabaseServiceFactoryTest extends JUnitSuite {

    private static Integer port = 5432;
    private static JHTTPLocationService jHttpLocationService;
    private static akka.actor.typed.ActorSystem<SpawnProtocol.Command> typedSystem;
    private static EmbeddedPostgres postgres;
    private static DatabaseServiceFactory dbFactory;
    private static ILocationService locationService;
    private static DSLContext testDsl;

    private String dbName = "postgres";

    @BeforeClass
    public static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        typedSystem = akka.actor.typed.ActorSystem.apply(SpawnProtocol.create(), "test");
//        untypedSystem = Adapter.toUntyped(typedSystem);
        Materializer mat = ActorMaterializerFactory.create(typedSystem);

        dbFactory = DBTestHelper.dbServiceFactory(typedSystem);
        postgres = DBTestHelper.postgres(port); // 0 is random port

        jHttpLocationService = new JHTTPLocationService();
        jHttpLocationService.beforeAll();

        locationService = JHttpLocationServiceFactory.makeLocalClient(typedSystem, mat);
        locationService.register(new TcpRegistration(DatabaseServiceConnection.value(), port)).get();

        // create database box_office
        testDsl = DBTestHelper.dslContext(typedSystem, port);
        testDsl.query("CREATE TABLE box_office(id SERIAL PRIMARY KEY)").executeAsync().toCompletableFuture().get(5, SECONDS);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        testDsl.query("DROP TABLE box_office").executeAsync().toCompletableFuture().get(5, SECONDS);
        postgres.close();
        typedSystem.terminate();
        Await.result(typedSystem.whenTerminated(), Duration.apply(5, SECONDS));
        jHttpLocationService.afterAll();
    }

    //DEOPSCSW-618: Integration with Location Service
    //DEOPSCSW-606: Examples for storing and using authentication information
    @Test
    public void shouldCreateDSLContextUsingLocationServiceAndDbName() throws InterruptedException, ExecutionException, TimeoutException {
        DSLContext dsl = dbFactory.jMakeDsl(locationService, dbName).get();
        List<String> resultSet =
                JooqHelper.fetchAsync(
                        dsl.resultQuery("select table_name from information_schema.tables"),
                        String.class)
                        .get(5, SECONDS);

        assertTrue(resultSet.contains("box_office"));
    }

    //DEOPSCSW-618: Integration with Location Service
    //DEOPSCSW-606: Examples for storing and using authentication information
    @Test
    public void shouldCreateDSLContextUsingLocationServiceDbNameUsernameHolderAndPasswordHolder() throws InterruptedException, ExecutionException, TimeoutException {
        DSLContext dsl = dbFactory.jMakeDsl(locationService, dbName, ReadUsernameHolder(), ReadPasswordHolder()).get();

        List<String> resultSet =
                JooqHelper.fetchAsync(
                        dsl.resultQuery("select table_name from information_schema.tables"),
                        String.class)
                        .get(5, SECONDS);

        assertTrue(resultSet.contains("box_office"));
    }

    @Test
    public void shouldCreateDSLContextUsingConfig() throws InterruptedException, ExecutionException, TimeoutException {
        DSLContext dsl = dbFactory.jMakeDsl().get();

        List<String> resultSet =
                JooqHelper.fetchAsync(
                        dsl.resultQuery("select table_name from information_schema.tables"),
                        String.class)
                        .get(5, SECONDS);

        assertTrue(resultSet.contains("box_office"));
    }

    //DEOPSCSW-605: Examples for multiple database support
    @Test
    public void shouldBeAbleToConnectToOtherDatabase() throws InterruptedException, ExecutionException, TimeoutException {
        // create a new database
        testDsl.query("CREATE DATABASE postgres2").executeAsync().toCompletableFuture().get(5, SECONDS);

        // make connection to the new database
        DSLContext dsl = dbFactory.jMakeDsl(locationService, "postgres2").get(5, SECONDS);

        // assert that box_office table is not present in newly created db
        List<String> resultSet =
                JooqHelper.fetchAsync(
                        dsl.resultQuery("select table_name from information_schema.tables"),
                        String.class)
                        .get(5, SECONDS);

        assertFalse(resultSet.contains("box_office"));

        // create a new table box_office in newly created db
        dsl.query("CREATE TABLE box_office(id SERIAL PRIMARY KEY)").executeAsync().toCompletableFuture().get(5, SECONDS);

        // assert the creation of table
        List<String> resultSet2 =
                JooqHelper.fetchAsync(
                        dsl.resultQuery("select table_name from information_schema.tables"),
                        String.class)
                        .get(5, SECONDS);

        assertTrue(resultSet2.contains("box_office"));

    }
}
