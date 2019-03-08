package csw.logging.client.javadsl;

import akka.actor.ActorSystem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.logging.api.javadsl.ILogger;
import csw.logging.api.models.LoggingLevels;
import csw.logging.api.models.RequestId;
import csw.logging.client.appenders.LogAppenderBuilder;
import csw.logging.client.commons.LoggingKeys$;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.utils.TestAppender;
import org.junit.*;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static csw.logging.client.utils.Eventually.eventually;

// DEOPSCSW-115: Format and control logging content
// DEOPSCSW-271: API change
// DEOPSCSW-278: Create Java API without arguments as suppliers
public class JLoggerImplAPITest extends JUnitSuite {

    private ILogger logger = JGenericLoggerFactory.getLogger(getClass());

    private java.time.Duration duration = java.time.Duration.ofSeconds(10);

    private String message = "Sample log message";
    private String exceptionMessage = "Sample exception message";
    private RuntimeException runtimeException = new RuntimeException(exceptionMessage);
    private RequestId requestId = JRequestId.id();
    private Map<String, Object> data = new HashMap<>() {
        {
            put(JKeys.OBS_ID, "foo_obs_id");
            put("key1", "value1");
            put("key2", "value2");
        }
    };
    private String className = getClass().getName();

    private static ActorSystem actorSystem = ActorSystem.create("base-system");
    private static LoggingSystem loggingSystem;

    private static List<JsonObject> logBuffer = new Vector<>();

    private static JsonObject parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, JsonElement.class).getAsJsonObject();
    }

    private static TestAppender testAppender = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static List<LogAppenderBuilder> appenderBuilders = Collections.singletonList(testAppender);

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("Logger-Test", "SNAPSHOT-1.0", "localhost", actorSystem, appenderBuilders);
        loggingSystem.setAppenders(scala.collection.JavaConverters.iterableAsScalaIterable(appenderBuilders).toList());
    }

    @Before
    public void beforeEach() {
        logBuffer.clear();
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

    private void testExceptionJsonObject(JsonObject jsonObject) {
        Assert.assertTrue(jsonObject.has("trace"));
        // DEOPSCSW-325: Include exception stack trace in stdout log message for exceptions
        Assert.assertTrue(jsonObject.has(LoggingKeys$.MODULE$.PLAINSTACK()));

        JsonObject messageBlock = jsonObject.get("trace").getAsJsonObject().get(LoggingKeys$.MODULE$.MESSAGE()).getAsJsonObject();
        Assert.assertEquals(exceptionMessage, messageBlock.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
    }

    private void testMessageWithMap(JsonObject fifthLogJsonObject) {
        Assert.assertEquals(this.message, fifthLogJsonObject.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
        Assert.assertEquals(data.get(JKeys.OBS_ID), fifthLogJsonObject.get(JKeys.OBS_ID).getAsString());
        Assert.assertEquals(data.get("key1"), fifthLogJsonObject.get("key1").getAsString());
        Assert.assertEquals(data.get("key2"), fifthLogJsonObject.get("key2").getAsString());
    }

    private void testCommonProperties(LoggingLevels.Level level) {
        logBuffer.forEach(log -> {
            String currentLogLevel = log.get(LoggingKeys$.MODULE$.SEVERITY()).getAsString();
            Assert.assertEquals(level, LoggingLevels.Level$.MODULE$.apply(currentLogLevel));
            Assert.assertTrue(log.has(LoggingKeys$.MODULE$.TIMESTAMP()));

            Assert.assertEquals(className, log.get(LoggingKeys$.MODULE$.CLASS()).getAsString());
        });
    }

    private void testAllOverloads() {
        JsonObject firstLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, firstLogJsonObject.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());

        JsonObject secondLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, secondLogJsonObject.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
        testExceptionJsonObject(secondLogJsonObject);

        JsonObject thirdLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, thirdLogJsonObject.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
        Assert.assertEquals(requestId.trackingId(), thirdLogJsonObject.get(LoggingKeys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());

        JsonObject fourthLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, fourthLogJsonObject.get(LoggingKeys$.MODULE$.MESSAGE()).getAsString());
        testExceptionJsonObject(fourthLogJsonObject);
        Assert.assertEquals(requestId.trackingId(), fourthLogJsonObject.get(LoggingKeys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());

        JsonObject fifthLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(fifthLogJsonObject);

        JsonObject sixthLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(sixthLogJsonObject);
        Assert.assertEquals(requestId.trackingId(), sixthLogJsonObject.get(LoggingKeys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());

        JsonObject seventhLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(seventhLogJsonObject);
        testExceptionJsonObject(seventhLogJsonObject);

        JsonObject eighthLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(eighthLogJsonObject);
        testExceptionJsonObject(eighthLogJsonObject);
        Assert.assertEquals(requestId.trackingId(), eighthLogJsonObject.get(LoggingKeys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());
    }

    @Test
    public void testOverloadedTraceLogLevel() {
        // test trace overloads with supplier
        logger.trace(() -> message);
        logger.trace(() -> message, runtimeException);
        logger.trace(() -> message, requestId);
        logger.trace(() -> message, runtimeException, requestId);
        logger.trace(() -> message, () -> data);
        logger.trace(() -> message, () -> data, requestId);
        logger.trace(() -> message, () -> data, runtimeException);
        logger.trace(() -> message, () -> data, runtimeException, requestId);
        // test trace overloads without supplier
        logger.trace(message);
        logger.trace(message, runtimeException);
        logger.trace(message, requestId);
        logger.trace(message, runtimeException, requestId);
        logger.trace(message, data);
        logger.trace(message, data, requestId);
        logger.trace(message, data, runtimeException);
        logger.trace(message, data, runtimeException, requestId);

        eventually(duration, () -> Assert.assertEquals(16, logBuffer.size()));

        testCommonProperties(LoggingLevels.TRACE$.MODULE$);
        testAllOverloads();
    }



    @Test
    public void testOverloadedDebugLogLevel() {
        // test debug overloads with supplier
        logger.debug(() -> message);
        logger.debug(() -> message, runtimeException);
        logger.debug(() -> message, requestId);
        logger.debug(() -> message, runtimeException, requestId);
        logger.debug(() -> message, () -> data);
        logger.debug(() -> message, () -> data, requestId);
        logger.debug(() -> message, () -> data, runtimeException);
        logger.debug(() -> message, () -> data, runtimeException, requestId);
        // test debug overloads without supplier
        logger.debug(message);
        logger.debug(message, runtimeException);
        logger.debug(message, requestId);
        logger.debug(message, runtimeException, requestId);
        logger.debug(message, data);
        logger.debug(message, data, requestId);
        logger.debug(message, data, runtimeException);
        logger.debug(message, data, runtimeException, requestId);

        eventually(duration, () -> Assert.assertEquals(16, logBuffer.size()));

        testCommonProperties(LoggingLevels.DEBUG$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedInfoLogLevel() {
        // test info overloads with supplier
        logger.info(() -> message);
        logger.info(() -> message, runtimeException);
        logger.info(() -> message, requestId);
        logger.info(() -> message, runtimeException, requestId);
        logger.info(() -> message, () -> data);
        logger.info(() -> message, () -> data, requestId);
        logger.info(() -> message, () -> data, runtimeException);
        logger.info(() -> message, () -> data, runtimeException, requestId);
        // test info overloads without supplier
        logger.info(message);
        logger.info(message, runtimeException);
        logger.info(message, requestId);
        logger.info(message, runtimeException, requestId);
        logger.info(message, data);
        logger.info(message, data, requestId);
        logger.info(message, data, runtimeException);
        logger.info(message, data, runtimeException, requestId);

        eventually(duration, () -> Assert.assertEquals(16, logBuffer.size()));

        testCommonProperties(LoggingLevels.INFO$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedWarnLogLevel() {
        // test warn overloads with supplier
        logger.warn(() -> message);
        logger.warn(() -> message, runtimeException);
        logger.warn(() -> message, requestId);
        logger.warn(() -> message, runtimeException, requestId);
        logger.warn(() -> message, () -> data);
        logger.warn(() -> message, () -> data, requestId);
        logger.warn(() -> message, () -> data, runtimeException);
        logger.warn(() -> message, () -> data, runtimeException, requestId);
        // test warn overloads without supplier
        logger.warn(message);
        logger.warn(message, runtimeException);
        logger.warn(message, requestId);
        logger.warn(message, runtimeException, requestId);
        logger.warn(message, data);
        logger.warn(message, data, requestId);
        logger.warn(message, data, runtimeException);
        logger.warn(message, data, runtimeException, requestId);

        eventually(duration, () -> Assert.assertEquals(16, logBuffer.size()));

        testCommonProperties(LoggingLevels.WARN$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedErrorLogLevel() {
        // test error overloads with supplier
        logger.error(() -> message);
        logger.error(() -> message, runtimeException);
        logger.error(() -> message, requestId);
        logger.error(() -> message, runtimeException, requestId);
        logger.error(() -> message, () -> data);
        logger.error(() -> message, () -> data, requestId);
        logger.error(() -> message, () -> data, runtimeException);
        logger.error(() -> message, () -> data, runtimeException, requestId);
        // test error overloads with supplier
        logger.error(message);
        logger.error(message, runtimeException);
        logger.error(message, requestId);
        logger.error(message, runtimeException, requestId);
        logger.error(message, data);
        logger.error(message, data, requestId);
        logger.error(message, data, runtimeException);
        logger.error(message, data, runtimeException, requestId);

        eventually(duration, () -> Assert.assertEquals(16, logBuffer.size()));

        testCommonProperties(LoggingLevels.ERROR$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedFatalLogLevel() {

        // test fatal overloads with supplier
        logger.fatal(() -> message);
        logger.fatal(() -> message, runtimeException);
        logger.fatal(() -> message, requestId);
        logger.fatal(() -> message, runtimeException, requestId);
        logger.fatal(() -> message, () -> data);
        logger.fatal(() -> message, () -> data, requestId);
        logger.fatal(() -> message, () -> data, runtimeException);
        logger.fatal(() -> message, () -> data, runtimeException, requestId);
        // test fatal overloads without supplier
        logger.fatal(message);
        logger.fatal(message, runtimeException);
        logger.fatal(message, requestId);
        logger.fatal(message, runtimeException, requestId);
        logger.fatal(message, data);
        logger.fatal(message, data, requestId);
        logger.fatal(message, data, runtimeException);
        logger.fatal(message, data, runtimeException, requestId);

        eventually(duration, () -> Assert.assertEquals(16, logBuffer.size()));

        testCommonProperties(LoggingLevels.FATAL$.MODULE$);
        testAllOverloads();
    }

}
