package csw.logging.utils;

import akka.actor.ActorRef;
import csw.logging.javadsl.ILogger;

import java.util.HashMap;
import java.util.Map;

public class JLogUtil {

    public static Map<String, String> logMsgMap = new HashMap<String, String>()
    {
        {
            put("trace", "logging at trace level");
            put("debug", "logging at debug level");
            put("info" , "logging at info level");
            put("warn" , "logging at warn level");
            put("error", "logging at error level");
            put("fatal", "logging at fatal level");
        }
    };

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
