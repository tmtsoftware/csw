package csw.logging.client.javadsl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.logging.api.models.LoggingLevels;
import csw.logging.client.appenders.LogAppenderBuilder;
import csw.logging.client.commons.LoggingKeys$;
import csw.logging.client.components.trombone.JTromboneHCDSupervisorActor;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.utils.JLogUtil;
import csw.logging.client.utils.TestAppender;
import org.junit.*;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static csw.logging.client.utils.Eventually.eventually;

public class ILoggerActorTest extends JUnitSuite {
    protected static ActorSystem actorSystem = ActorSystem.create("base-system");
    protected static LoggingSystem loggingSystem;

    protected static List<JsonObject> logBuffer = new ArrayList<>();

    protected static JsonObject parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, JsonElement.class).getAsJsonObject();
    }

    protected static TestAppender testAppender     = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static List<LogAppenderBuilder> appenderBuilders = Collections.singletonList(testAppender);

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
    public void testDefaultLogConfigurationForActor() {
        ActorRef tromboneActor = actorSystem.actorOf(Props.create(JTromboneHCDSupervisorActor.class, new JLoggerFactory("jTromboneHcdActor")), "JTromboneActor");
        String actorPath = tromboneActor.path().toString();
        String className = JTromboneHCDSupervisorActor.class.getName();

        JLogUtil.sendLogMsgToActorInBulk(tromboneActor);

        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(4, logBuffer.size()));

        logBuffer.forEach(log -> {
            Assert.assertEquals("jTromboneHcdActor", log.get(LoggingKeys$.MODULE$.COMPONENT_NAME()).getAsString());
            Assert.assertEquals(actorPath, log.get(LoggingKeys$.MODULE$.ACTOR()).getAsString());

            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.SEVERITY()));
            String severity = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();

            Assert.assertEquals(severity, log.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
            Assert.assertEquals(className, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());

            LoggingLevels.Level currentLogLevel = LoggingLevels.Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(LoggingLevels.INFO$.MODULE$));
        });
    }

}
