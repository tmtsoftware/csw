package csw.logging.client.javadsl;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Props;
import akka.actor.typed.SpawnProtocol;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.logging.models.Level;
import csw.logging.models.Level$;
import csw.logging.client.LogCommand;
import csw.logging.client.appenders.LogAppenderBuilder;
import csw.logging.client.commons.AkkaTypedExtension;
import csw.logging.client.commons.LoggingKeys$;
import csw.logging.client.components.iris.JIrisSupervisorMutableActor;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.utils.TestAppender;
import csw.prefix.models.Prefix;
import org.junit.*;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static csw.logging.client.utils.Eventually.eventually;

// DEOPSCSW-280 SPIKE: Introduce Akkatyped in logging
// CSW-78: PrefixRedesign for logging
// CSW-86: Subsystem should be case-insensitive
public class ILoggerMutableActorTest extends JUnitSuite {
    protected static ActorSystem<SpawnProtocol.Command> actorSystem = ActorSystem.create(SpawnProtocol.create(), "base-system");
    protected static LoggingSystem loggingSystem;

    protected static List<JsonObject> logBuffer = new ArrayList<>();

    protected static JsonObject parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, JsonElement.class).getAsJsonObject();
    }

    protected static TestAppender testAppender = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static final List<LogAppenderBuilder> appenderBuilders = List.of(testAppender);

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("Logger-Test", "SNAPSHOT-1.0", "localhost", actorSystem, appenderBuilders);
    }

    @After
    public void afterEach() {
        logBuffer.clear();
    }

    @AfterClass
    public static void teardown() throws Exception {
        loggingSystem.javaStop().get();
        actorSystem.terminate();
        Await.result(actorSystem.whenTerminated(), Duration.create(10, TimeUnit.SECONDS));
    }

    @Test
    public void testDefaultLogConfigurationForActor__DEOPSCSW_280() {

        ActorRef<LogCommand> irisTyped =
                AkkaTypedExtension
                        .UserActorFactory(actorSystem)
                        .spawn(
                                JIrisSupervisorMutableActor.irisBeh(Prefix.apply("csw.jIRISTyped")),
                                "irisTyped",
                                Props.empty()
                        );

        String actorPath = irisTyped.path().toString();
        String className = JIrisSupervisorMutableActor.class.getName();

        sendLogMsgToTypedActorInBulk(irisTyped);

        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(4, logBuffer.size()));

        logBuffer.forEach(log -> {
            Assert.assertEquals("jIRISTyped", log.get(LoggingKeys$.MODULE$.COMPONENT_NAME()).getAsString());
            Assert.assertEquals("CSW", log.get(LoggingKeys$.MODULE$.SUBSYSTEM()).getAsString());
            Assert.assertEquals("CSW.jIRISTyped", log.get(LoggingKeys$.MODULE$.PREFIX()).getAsString());
            Assert.assertEquals(actorPath, log.get(LoggingKeys$.MODULE$.ACTOR()).getAsString());

            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.SEVERITY()));
            String severity = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();

            Assert.assertEquals(severity, log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase());
            Assert.assertEquals(className, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());

            Level currentLogLevel = Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(Level.INFO$.MODULE$));
        });
    }

    private static void sendLogMsgToTypedActorInBulk(akka.actor.typed.ActorRef<LogCommand> actorRef) {
        actorRef.tell(JLogCommand.LogTrace);
        actorRef.tell(JLogCommand.LogDebug);
        actorRef.tell(JLogCommand.LogInfo);
        actorRef.tell(JLogCommand.LogWarn);
        actorRef.tell(JLogCommand.LogError);
        actorRef.tell(JLogCommand.LogFatal);
    }
}
