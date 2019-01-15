package csw.logging.core.javadsl;

import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.logging.core.appenders.LogAppenderBuilder;
import csw.logging.core.commons.LoggingKeys$;
import csw.logging.core.components.iris.JIrisSupervisorMutableActor;
import csw.logging.core.internal.LoggingLevels;
import csw.logging.core.internal.LoggingSystem;
import csw.logging.core.LogCommand;
import csw.logging.core.utils.TestAppender;
import org.junit.*;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

// DEOPSCSW-280 SPIKE: Introduce Akkatyped in logging
public class ILoggerMutableActorTest extends JUnitSuite {
    protected static ActorSystem actorSystem = ActorSystem.create("base-system");
    protected static LoggingSystem loggingSystem;

    protected static List<JsonObject> logBuffer = new ArrayList<>();

    protected static JsonObject parse(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonElement.class).getAsJsonObject();
        return jsonObject;
    }

    protected static TestAppender testAppender     = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    protected static List<LogAppenderBuilder> appenderBuilders = Arrays.asList(testAppender);


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
        Await.result(actorSystem.terminate(), Duration.create(10, TimeUnit.SECONDS));
    }
    @Test
    public void testDefaultLogConfigurationForActor() throws InterruptedException {

        ActorRef<LogCommand> irisTyped = Adapter.spawn(actorSystem, JIrisSupervisorMutableActor.irisBeh("jIRISTyped"), "irisTyped");

        String actorPath = irisTyped.path().toString();
        String className = JIrisSupervisorMutableActor.class.getName();

        sendLogMsgToTypedActorInBulk(irisTyped);

        Thread.sleep(300);

        Assert.assertEquals(4, logBuffer.size());
        logBuffer.forEach(log -> {
            Assert.assertEquals("jIRISTyped", log.get(LoggingKeys$.MODULE$.COMPONENT_NAME()).getAsString());
            Assert.assertEquals(actorPath, log.get(LoggingKeys$.MODULE$.ACTOR()).getAsString());

            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.SEVERITY()));
            String severity = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();

            Assert.assertEquals(severity, log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase());
            Assert.assertEquals(className, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());

            LoggingLevels.Level currentLogLevel = LoggingLevels.Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(LoggingLevels.INFO$.MODULE$));
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
