package csw.services.logging.javadsl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.components.iris.JIrisActorLogger;
import csw.services.logging.components.iris.JIrisSupervisorActor;
import csw.services.logging.components.iris.JIrisTLA;
import csw.services.logging.components.trombone.JTromboneHCDSupervisorActor;
import csw.services.logging.components.trombone.JTromboneHCDTLA;
import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.utils.JGenericActor;
import csw.services.logging.utils.JGenericSimple;
import csw.services.logging.utils.JLogUtil;
import csw.services.logging.utils.TestAppender;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ILoggerTest {
    private static ActorSystem actorSystem = ActorSystem.create("base-system");
    private static LoggingSystem loggingSystem;

    private static List<JsonObject> logBuffer = new ArrayList<>();

    private static JsonObject parse(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonElement.class).getAsJsonObject();
        return jsonObject;
    }

    private static TestAppender testAppender     = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static List<LogAppenderBuilder> appenderBuilders = Arrays.asList(testAppender);

    private static Map<String, List<JsonObject>> componentLogBuffer = new HashMap<>();
    private static List<JsonObject> genericLogBuffer = new ArrayList<>();
    private static List<JsonObject> irisLogBuffer = new ArrayList<>();
    private static List<JsonObject> tromboneHcdLogBuffer = new ArrayList<>();

    private static ActorRef irisSupervisorActor = actorSystem.actorOf(Props.create(JIrisSupervisorActor.class), "JIRISActor");
    private static ActorRef tromboneSupervisorActor = actorSystem.actorOf(Props.create(JTromboneHCDSupervisorActor.class), "JTromboneActor");
    private static ActorRef genericActor = actorSystem.actorOf(Props.create(JGenericActor.class), "JGenericActor");


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

    private void allComponentsStartLogging() {
        JIrisTLA irisTLA = new JIrisTLA();
        JGenericSimple genericSimple = new JGenericSimple();

        //componentName = jIRIS
        JLogUtil.sendLogMsgToActorInBulk(irisSupervisorActor);
        irisTLA.startLogging();
        //componentName = jTromboneHcdActor
        JLogUtil.sendLogMsgToActorInBulk(tromboneSupervisorActor);
        //generic logging
        JLogUtil.sendLogMsgToActorInBulk(genericActor);
        genericSimple.startLogging();
    }

    private void splitAndGroupLogs() {
        // clear all logs
        componentLogBuffer.clear();
        irisLogBuffer.clear();
        genericLogBuffer.clear();
        tromboneHcdLogBuffer.clear();

        logBuffer.forEach(log -> {
            if (log.has("@componentName")) {
                String name = log.get("@componentName").getAsString();
                componentLogBuffer.computeIfAbsent(name, k -> new ArrayList<>()).add(log);
            } else
                genericLogBuffer.add(log);
        });

        irisLogBuffer = componentLogBuffer.get(JIrisActorLogger.NAME);
        tromboneHcdLogBuffer = componentLogBuffer.get("jTromboneHcdActor");

        logBuffer.clear();
    }

    private void testLogBuffer(List<JsonObject> logBuffer, LoggingLevels.Level configuredLogLevel) {
        logBuffer.forEach( log -> {
            String currentLogLevel = log.get("@severity").getAsString().toLowerCase();
            Assert.assertTrue(LoggingLevels.Level$.MODULE$.apply(currentLogLevel).$greater$eq(configuredLogLevel));
        });
    }

    @Test
    public void testDefaultLogConfigurationAndFilter() throws InterruptedException {
        JTromboneHCDTLA jTromboneHCD = new JTromboneHCDTLA();
        String tromboneHcdClassName = jTromboneHCD.getClass().getName();

        jTromboneHCD.startLogging();
        Thread.sleep(300);

        logBuffer.forEach(log -> {
            Assert.assertEquals("tromboneHcd", log.get("@componentName").getAsString());

            Assert.assertTrue(log.has("@severity"));
            String severity = log.get("@severity").getAsString().toLowerCase();

            Assert.assertEquals(JLogUtil.logMsgMap.get(severity), log.get("message").getAsString());
            Assert.assertEquals(tromboneHcdClassName, log.get("class").getAsString());

            LoggingLevels.Level currentLogLevel = LoggingLevels.Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(LoggingLevels.DEBUG$.MODULE$));
        });
    }

    // This test simulates single jvm multiple components use cases
    @Test
    public void testLogLevel() throws InterruptedException {

        allComponentsStartLogging();
        Thread.sleep(200);

        splitAndGroupLogs();

        Assert.assertEquals(4, irisLogBuffer.size());
        testLogBuffer(irisLogBuffer, LoggingLevels.ERROR$.MODULE$);

        Assert.assertEquals(4, tromboneHcdLogBuffer.size());
        testLogBuffer(tromboneHcdLogBuffer, LoggingLevels.INFO$.MODULE$);

        Assert.assertEquals(12, genericLogBuffer.size());
        testLogBuffer(genericLogBuffer, LoggingLevels.TRACE$.MODULE$);
        
        loggingSystem.setComponentLogLevel(JIrisActorLogger.NAME, LoggingLevels.FATAL$.MODULE$);

        allComponentsStartLogging();
        Thread.sleep(200);

        splitAndGroupLogs();

        Assert.assertEquals(2, irisLogBuffer.size());
        testLogBuffer(irisLogBuffer, LoggingLevels.FATAL$.MODULE$);

        Assert.assertEquals(4, tromboneHcdLogBuffer.size());
        testLogBuffer(tromboneHcdLogBuffer, LoggingLevels.INFO$.MODULE$);

        Assert.assertEquals(12, genericLogBuffer.size());
        testLogBuffer(genericLogBuffer, LoggingLevels.TRACE$.MODULE$);
    }
}
