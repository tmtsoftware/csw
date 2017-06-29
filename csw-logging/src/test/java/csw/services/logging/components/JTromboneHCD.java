package csw.services.logging.components;

import csw.services.logging.javadsl.ILogger;

import java.util.Map;

public class JTromboneHCD implements JTromboneHCDLogger {
    private ILogger log = getLogger();

    public void startLogging(Map<String, String> logMsgMap) {
        log.trace(() -> logMsgMap.get("trace"));
        log.debug(() -> logMsgMap.get("debug"));
        log.info(() -> logMsgMap.get("info"));
        log.warn(() -> logMsgMap.get("warn"));
        log.error(() -> logMsgMap.get("error"));
        log.fatal(() -> logMsgMap.get("fatal"));
    }
}
