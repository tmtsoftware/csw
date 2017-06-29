package csw.services.logging.javadsl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.components.JTromboneHCDActor;
import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.utils.TestAppender;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ILoggerActorTest {

    private static ActorSystem actorSystem = ActorSystem.create("logging");
    private static LoggingSystem loggingSystem;

    public static List<JsonObject> logBuffer = new ArrayList<>();

    public static JsonObject parse(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonElement.class).getAsJsonObject();
        return jsonObject;
    }

    private static TestAppender testAppender     = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static List<LogAppenderBuilder> appenderBuilders = Arrays.asList(testAppender);

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("LoggerActor-Test", "SNAPSHOT-1.0", "localhost", actorSystem, appenderBuilders);
    }

    @After
    public void afterEach() {
        logBuffer.clear();
    }

    @AfterClass
    public static void teardown() throws Exception {
        loggingSystem.javaStop().get();
        Await.result(actorSystem.terminate(), Duration.create(5, TimeUnit.SECONDS));
    }

    @Test
    public void testDefaultLogConfigurationAndFilterForActor() throws InterruptedException {
        ActorRef tromboneActor = actorSystem.actorOf(Props.create(JTromboneHCDActor.class), "JTromboneActor");
        String actorPath = tromboneActor.path().toString();
        String className = JTromboneHCDActor.class.getName();

        tromboneActor.tell("trace", ActorRef.noSender());
        tromboneActor.tell("debug", ActorRef.noSender());
        tromboneActor.tell("info", ActorRef.noSender());
        tromboneActor.tell("warn", ActorRef.noSender());
        tromboneActor.tell("error", ActorRef.noSender());
        tromboneActor.tell("fatal", ActorRef.noSender());

        Thread.sleep(300);

        logBuffer.forEach(log -> {
            Assert.assertEquals("jTromboneHcdActor", log.get("@componentName").getAsString());
            Assert.assertEquals(actorPath, log.get("actor").getAsString());

            Assert.assertTrue(log.has("@severity"));
            String severity = log.get("@severity").getAsString().toLowerCase();

            Assert.assertEquals(severity, log.get("message").getAsString());
            Assert.assertEquals(className, log.get("class").getAsString());

            LoggingLevels.Level currentLogLevel = LoggingLevels.Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(LoggingLevels.TRACE$.MODULE$));
        });
    }

}
