package csw.services.logging.javadsl;

import akka.actor.ActorSystem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.commons.Keys$;
import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.scaladsl.RequestId;
import csw.services.logging.utils.JGenericSimple;
import csw.services.logging.utils.TestAppender;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class JLogerAPITest extends JGenericSimple {

    private ILogger logger = getLogger();

    private String message = "Sample log message";
    private String exceptionMessage = "Sample exception message";
    private RuntimeException runtimeException = new RuntimeException(exceptionMessage);
    private RequestId requestId = JRequestId.id();
    private Map<String, Object> data = new HashMap<String, Object>() {
        {
            put("key1", "value1");
            put("key2", "value2");
        }
    };
    private String className = getClass().getName();

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

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("Logger-Test", "SNAPSHOT-1.0", "localhost", actorSystem, appenderBuilders);
        loggingSystem.setAppenders(scala.collection.JavaConverters.iterableAsScalaIterable(appenderBuilders).toList());
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
        JsonObject messageBlock = jsonObject.get("trace").getAsJsonObject().get(Keys$.MODULE$.MESSAGE()).getAsJsonObject();
        Assert.assertEquals(exceptionMessage, messageBlock.get(Keys$.MODULE$.MESSAGE()).getAsString());
    }

    private void testMessageWithMap(JsonObject fifthLogJsonObject) {
        JsonObject messageJasonObject = fifthLogJsonObject.get(Keys$.MODULE$.MESSAGE()).getAsJsonObject();
        Assert.assertEquals(this.message, messageJasonObject.get(Keys$.MODULE$.MSG()).getAsString());
        Assert.assertEquals(data.get("key1"), messageJasonObject.get("key1").getAsString());
        Assert.assertEquals(data.get("key2"), messageJasonObject.get("key2").getAsString());
    }

    private void testCommonProperties(LoggingLevels.Level level) {
        logBuffer.forEach(log -> {
            String currentLogLevel = log.get(Keys$.MODULE$.SEVERITY()).getAsString();
            Assert.assertEquals(level, LoggingLevels.Level$.MODULE$.apply(currentLogLevel));
            Assert.assertTrue(log.has(Keys$.MODULE$.TIMESTAMP()));

            Assert.assertEquals(className, log.get(Keys$.MODULE$.CLASS()).getAsString());
        });
    }

    private void testAllOverloads() {
        JsonObject firstLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, firstLogJsonObject.get(Keys$.MODULE$.MESSAGE()).getAsString());

        JsonObject secondLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, secondLogJsonObject.get(Keys$.MODULE$.MESSAGE()).getAsString());
        testExceptionJsonObject(secondLogJsonObject);

        JsonObject thirdLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, thirdLogJsonObject.get(Keys$.MODULE$.MESSAGE()).getAsString());
        Assert.assertEquals(requestId.trackingId(), thirdLogJsonObject.get(Keys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());

        JsonObject fourthLogJsonObject = logBuffer.remove(0);
        Assert.assertEquals(message, fourthLogJsonObject.get(Keys$.MODULE$.MESSAGE()).getAsString());
        testExceptionJsonObject(fourthLogJsonObject);
        Assert.assertEquals(requestId.trackingId(), fourthLogJsonObject.get(Keys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());

        JsonObject fifthLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(fifthLogJsonObject);

        JsonObject sixthLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(sixthLogJsonObject);
        Assert.assertEquals(requestId.trackingId(), sixthLogJsonObject.get(Keys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());

        JsonObject seventhLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(seventhLogJsonObject);
        testExceptionJsonObject(seventhLogJsonObject);

        JsonObject eighthLogJsonObject = logBuffer.remove(0);
        testMessageWithMap(eighthLogJsonObject);
        testExceptionJsonObject(eighthLogJsonObject);
        Assert.assertEquals(requestId.trackingId(), eighthLogJsonObject.get(Keys$.MODULE$.TRACE_ID()).getAsJsonArray().get(0).getAsString());
    }

    @Test
    public void testOverloadedTraceLogLevel() throws InterruptedException {

        logger.trace(() -> message);
        logger.trace(() -> message, runtimeException);
        logger.trace(() -> message, requestId);
        logger.trace(() -> message, runtimeException, requestId);
        logger.trace(() -> message, () -> data);
        logger.trace(() -> message, () -> data, requestId);
        logger.trace(() -> message, () -> data, runtimeException);
        logger.trace(() -> message, () -> data, runtimeException, requestId);

        Thread.sleep(200);

        testCommonProperties(LoggingLevels.TRACE$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedDebugLogLevel() throws InterruptedException {

        logger.debug(() -> message);
        logger.debug(() -> message, runtimeException);
        logger.debug(() -> message, requestId);
        logger.debug(() -> message, runtimeException, requestId);
        logger.debug(() -> message, () -> data);
        logger.debug(() -> message, () -> data, requestId);
        logger.debug(() -> message, () -> data, runtimeException);
        logger.debug(() -> message, () -> data, runtimeException, requestId);

        Thread.sleep(200);

        testCommonProperties(LoggingLevels.DEBUG$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedInfoLogLevel() throws InterruptedException {

        logger.info(() -> message);
        logger.info(() -> message, runtimeException);
        logger.info(() -> message, requestId);
        logger.info(() -> message, runtimeException, requestId);
        logger.info(() -> message, () -> data);
        logger.info(() -> message, () -> data, requestId);
        logger.info(() -> message, () -> data, runtimeException);
        logger.info(() -> message, () -> data, runtimeException, requestId);

        Thread.sleep(200);

        testCommonProperties(LoggingLevels.INFO$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedWarnLogLevel() throws InterruptedException {

        logger.warn(() -> message);
        logger.warn(() -> message, runtimeException);
        logger.warn(() -> message, requestId);
        logger.warn(() -> message, runtimeException, requestId);
        logger.warn(() -> message, () -> data);
        logger.warn(() -> message, () -> data, requestId);
        logger.warn(() -> message, () -> data, runtimeException);
        logger.warn(() -> message, () -> data, runtimeException, requestId);

        Thread.sleep(200);

        testCommonProperties(LoggingLevels.WARN$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedErrorLogLevel() throws InterruptedException {

        logger.error(() -> message);
        logger.error(() -> message, runtimeException);
        logger.error(() -> message, requestId);
        logger.error(() -> message, runtimeException, requestId);
        logger.error(() -> message, () -> data);
        logger.error(() -> message, () -> data, requestId);
        logger.error(() -> message, () -> data, runtimeException);
        logger.error(() -> message, () -> data, runtimeException, requestId);

        Thread.sleep(200);

        testCommonProperties(LoggingLevels.ERROR$.MODULE$);
        testAllOverloads();
    }

    @Test
    public void testOverloadedFatalLogLevel() throws InterruptedException {

        logger.fatal(() -> message);
        logger.fatal(() -> message, runtimeException);
        logger.fatal(() -> message, requestId);
        logger.fatal(() -> message, runtimeException, requestId);
        logger.fatal(() -> message, () -> data);
        logger.fatal(() -> message, () -> data, requestId);
        logger.fatal(() -> message, () -> data, runtimeException);
        logger.fatal(() -> message, () -> data, runtimeException, requestId);

        Thread.sleep(200);

        testCommonProperties(LoggingLevels.FATAL$.MODULE$);
        testAllOverloads();
    }

}
