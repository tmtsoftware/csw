package example.testkit;

import csw.testkit.DatabaseTestKit;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.jooq.DSLContext;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//#junit-database-testkit
class JUnitDatabaseTestKitExampleTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.DatabaseServer));
    private DatabaseTestKit databaseTestKit = testKit.frameworkTestKit().databaseTestKit();

    @Test
    public void testUsingDslContext() {
        DSLContext queryDsl = databaseTestKit.dslContext("postgres");
        // ... queries, assertions etc.
    }

    @Test
    public void testUsingDatabaseServiceFactory() throws ExecutionException, InterruptedException, TimeoutException {
        DSLContext queryDsl = databaseTestKit.databaseServiceFactory("postgres", "postgres").jMakeDsl(testKit.jLocationService(), "postgres").get(5, TimeUnit.SECONDS);
        // ... queries, assertions etc.
    }
}
//#junit-database-testkit
