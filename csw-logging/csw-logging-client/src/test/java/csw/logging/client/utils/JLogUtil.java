package csw.logging.client.utils;

import akka.actor.ActorRef;
import csw.logging.api.javadsl.ILogger;

import java.util.Map;

public class JLogUtil {

    public static Map<String, String> logMsgMap = Map.of(
        "trace", "logging at trace level",
        "debug", "logging at debug level",
        "info" , "logging at info level",
        "warn" , "logging at warn level",
        "error", "logging at error level",
        "fatal", "logging at fatal level"
    );

    public static void logInBulk(ILogger logger) {
        logger.trace(() -> logMsgMap.get("trace"));
        logger.debug(() -> logMsgMap.get("debug"));
        logger.info(() -> logMsgMap.get("info"));
        logger.warn(() -> logMsgMap.get("warn"));
        logger.error(() -> logMsgMap.get("error"));
        logger.fatal(() -> logMsgMap.get("fatal"));
    }

    public static void sendLogMsgToActorInBulk(ActorRef actorRef) {
        actorRef.tell("trace", ActorRef.noSender());
        actorRef.tell("debug", ActorRef.noSender());
        actorRef.tell("info", ActorRef.noSender());
        actorRef.tell("warn", ActorRef.noSender());
        actorRef.tell("error", ActorRef.noSender());
        actorRef.tell("fatal", ActorRef.noSender());
    }

}
