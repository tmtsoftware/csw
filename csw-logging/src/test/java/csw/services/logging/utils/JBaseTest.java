package csw.services.logging.utils;

import akka.actor.ActorSystem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.javadsl.JLoggingSystemFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

// todo: extend this class from all the test. Currently test fails if extended from this class. (Might be because of concurrency)
public class JBaseTest {

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
}
