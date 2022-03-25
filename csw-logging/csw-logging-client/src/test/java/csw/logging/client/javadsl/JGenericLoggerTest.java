package csw.logging.client.javadsl;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.logging.api.javadsl.ILogger;
import csw.logging.models.Level;
import csw.logging.models.Level$;
import csw.logging.client.appenders.LogAppenderBuilder;
import csw.logging.client.commons.AkkaTypedExtension;
import csw.logging.client.commons.LoggingKeys$;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.utils.JGenericActor;
import csw.logging.client.utils.JLogUtil;
import csw.logging.client.utils.TestAppender;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static csw.logging.client.utils.Eventually.eventually;

// DEOPSCSW-316: Improve Logger accessibility for component developers
public class JGenericLoggerTest {
    private static final ActorSystem<SpawnProtocol.Command> actorSystem = ActorSystem.create(SpawnProtocol.create(), "base-system");
    private static LoggingSystem loggingSystem;

    private static final List<JsonObject> logBuffer = new ArrayList<>();

    private static JsonObject parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, JsonElement.class).getAsJsonObject();
    }

    private static final TestAppender testAppender = new TestAppender(x -> {
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

    private class JGenericLoggerUtil {
        private final ILogger logger = JGenericLoggerFactory.getLogger(getClass());

        public void start() {
            JLogUtil.logInBulk(logger);
        }
    }

    // DEOPSCSW-277: Java nested class name is not logged correctly in log messages.
    @Test
    public void testGenericLoggerWithoutComponentName__DEOPSCSW_277_DEOPSCSW_316() {
        String className = JGenericLoggerTest.JGenericLoggerUtil.class.getName();
        new JGenericLoggerTest.JGenericLoggerUtil().start();

        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(6, logBuffer.size()));

        logBuffer.forEach(log -> {
            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.SEVERITY()));
            String severity = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();

            Assert.assertEquals(JLogUtil.logMsgMap.get(severity), log.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
            Assert.assertEquals(className, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());
            Level currentLogLevel = Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(Level.TRACE$.MODULE$));
        });
    }

    @Test
    public void testGenericLoggerActorWithoutComponentName_DEOPSCSW_316() {

        AkkaTypedExtension.UserActorFactory userActorFactory = AkkaTypedExtension.UserActorFactory(actorSystem);

        ActorRef<String> utilActor = userActorFactory.<String>spawn(JGenericActor.behavior, "JActorUtil", akka.actor.typed.Props.empty());

        String actorPath = utilActor.path().toString();
        String className = JGenericActor.class.getName();

        JLogUtil.sendLogMsgToActorInBulk(utilActor);

        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(6, logBuffer.size()));
        logBuffer.forEach(log -> {
            Assert.assertEquals(actorPath, log.get(LoggingKeys$.MODULE$.ACTOR()).getAsString());

            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.SEVERITY()));
            String severity = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();

            Assert.assertEquals(severity, log.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
            Assert.assertEquals(className, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());

            Level currentLogLevel = Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(Level.TRACE$.MODULE$));
        });
    }
}
