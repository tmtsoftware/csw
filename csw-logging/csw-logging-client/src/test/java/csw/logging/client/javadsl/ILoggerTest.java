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
import csw.logging.client.appenders.LogAppenderBuilder;
import csw.logging.client.commons.AkkaTypedExtension;
import csw.logging.client.commons.LoggingKeys$;
import csw.logging.client.components.iris.JIrisSupervisorActor;
import csw.logging.client.components.iris.JIrisTLA;
import csw.logging.client.components.trombone.JTromboneHCDSupervisorActor;
import csw.logging.client.components.trombone.JTromboneHCDTLA;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.utils.JGenericActor;
import csw.logging.client.utils.JGenericSimple;
import csw.logging.client.utils.JLogUtil;
import csw.logging.client.utils.TestAppender;
import org.junit.*;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static csw.logging.client.utils.Eventually.eventually;

// DEOPSCSW-316: Improve Logger accessibility for component developers
public class ILoggerTest extends JUnitSuite {
    private static ActorSystem actorSystem = ActorSystem.create(SpawnProtocol.create(), "base-system");
    private static LoggingSystem loggingSystem;

    private static List<JsonObject> logBuffer = new ArrayList<>();

    private static JsonObject parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, JsonElement.class).getAsJsonObject();
    }

    private static TestAppender testAppender = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static List<LogAppenderBuilder> appenderBuilders = List.of(testAppender);

    private static Map<String, List<JsonObject>> componentLogBuffer = new HashMap<>();
    private static List<JsonObject> genericLogBuffer = new ArrayList<>();
    private static List<JsonObject> irisLogBuffer = new ArrayList<>();
    private static List<JsonObject> tromboneHcdLogBuffer = new ArrayList<>();

    private static AkkaTypedExtension.UserActorFactory userActorFactory = AkkaTypedExtension.UserActorFactory(actorSystem);

    private static ActorRef<String> irisSupervisorActor = userActorFactory.<String>spawn(JIrisSupervisorActor.behavior, "JIRISActor", Props.empty());
    private static ActorRef<String> tromboneSupervisorActor = userActorFactory.<String>spawn(JTromboneHCDSupervisorActor.behavior(new JLoggerFactory("jTromboneHcdActor")), "JTromboneActor", Props.empty());
    private static ActorRef<String> genericActor = userActorFactory.<String>spawn(JGenericActor.behavior, "JGenericActor", Props.empty());

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("Logger-Test", "SNAPSHOT-1.0", "localhost", actorSystem, appenderBuilders);
        loggingSystem.setAppenders(scala.jdk.CollectionConverters.ListHasAsScala(appenderBuilders).asScala().toList());
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

    private void allComponentsStartLogging() {
        JIrisTLA irisTLA = new JIrisTLA(new JLoggerFactory("jIRIS"));
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
            if (log.has(LoggingKeys$.MODULE$.COMPONENT_NAME())) {
                String name = log.get(LoggingKeys$.MODULE$.COMPONENT_NAME()).getAsString();
                componentLogBuffer.computeIfAbsent(name, k -> new ArrayList<>()).add(log);
            } else
                genericLogBuffer.add(log);
        });

        irisLogBuffer = componentLogBuffer.get("jIRIS");
        tromboneHcdLogBuffer = componentLogBuffer.get("jTromboneHcdActor");

        logBuffer.clear();
    }

    private void testLogBuffer(List<JsonObject> logBuffer, Level configuredLogLevel) {
        logBuffer.forEach(log -> {
            String currentLogLevel = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();
            Assert.assertTrue(Level$.MODULE$.apply(currentLogLevel).$greater$eq(configuredLogLevel));
        });
    }

    @Test
    public void testDefaultLogConfigurationAndDefaultComponentLogLevel() throws InterruptedException {
        JTromboneHCDTLA jTromboneHCD = new JTromboneHCDTLA(new JLoggerFactory("tromboneHcd"));
        String tromboneHcdClassName = jTromboneHCD.getClass().getName();

        jTromboneHCD.startLogging();
        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(5, logBuffer.size()));

        logBuffer.forEach(log -> {
            Assert.assertEquals("tromboneHcd", log.get(LoggingKeys$.MODULE$.COMPONENT_NAME()).getAsString());

            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.SEVERITY()));
            String severity = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString().toLowerCase();

            Assert.assertEquals(JLogUtil.logMsgMap.get(severity), log.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
            Assert.assertEquals(tromboneHcdClassName, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());

            Level currentLogLevel = Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(Level.DEBUG$.MODULE$));
        });
    }

    // This test simulates single jvm multiple components use cases
    // DEOPSCSW-117: Provide unique name for each logging instance of components
    // DEOPSCSW-127: Runtime update for logging characteristics
    @Test
    public void testLogLevelOfMultipleComponentsInSingleContainer() throws InterruptedException {

        allComponentsStartLogging();
        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(20, logBuffer.size()));

        splitAndGroupLogs();

        // Log level of IRIS component is ERROR in config file
        Assert.assertEquals(4, irisLogBuffer.size());
        testLogBuffer(irisLogBuffer, Level.ERROR$.MODULE$);

        // Log level of jTromboneHcd component is ERROR in config file
        Assert.assertEquals(4, tromboneHcdLogBuffer.size());
        testLogBuffer(tromboneHcdLogBuffer, Level.INFO$.MODULE$);

        // Default log level is TRACE
        Assert.assertEquals(12, genericLogBuffer.size());
        testLogBuffer(genericLogBuffer, Level.TRACE$.MODULE$);

        // Set log level of IRIS component to FATAL
        loggingSystem.setComponentLogLevel("jIRIS", Level.FATAL$.MODULE$);

        allComponentsStartLogging();
        eventually(java.time.Duration.ofSeconds(10), () -> Assert.assertEquals(18, logBuffer.size()));

        splitAndGroupLogs();

        // Updated log level of IRIS is FATAL
        Assert.assertEquals(2, irisLogBuffer.size());
        testLogBuffer(irisLogBuffer, Level.FATAL$.MODULE$);

        // Log level of jTromboneHcd component is unaffected
        Assert.assertEquals(4, tromboneHcdLogBuffer.size());
        testLogBuffer(tromboneHcdLogBuffer, Level.INFO$.MODULE$);

        // Default log level is unaffected
        Assert.assertEquals(12, genericLogBuffer.size());
        testLogBuffer(genericLogBuffer, Level.TRACE$.MODULE$);
    }
}
