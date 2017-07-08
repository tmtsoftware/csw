package csw.services.logging.javadsl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.components.trombone.JTromboneHCDSupervisorActor;
import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.utils.JLogUtil;
import csw.services.logging.utils.TestAppender;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ILoggerActorTest {
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
        ActorRef tromboneActor = actorSystem.actorOf(Props.create(JTromboneHCDSupervisorActor.class), "JTromboneActor");
        String actorPath = tromboneActor.path().toString();
        String className = JTromboneHCDSupervisorActor.class.getName();

        JLogUtil.sendLogMsgToActorInBulk(tromboneActor);

        Thread.sleep(300);
/*
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
        */
    }

}
