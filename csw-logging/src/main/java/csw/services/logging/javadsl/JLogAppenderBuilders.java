package csw.services.logging.javadsl;

import csw.services.logging.appenders.FileAppender$;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.appenders.StdOutAppender$;

/**
 * Helper class for Java to get the handle of csw provided appenders
 */
public class JLogAppenderBuilders {

    /**
     * Represents StdOut as appender for logs
     */
    public static final LogAppenderBuilder StdOutAppender = StdOutAppender$.MODULE$;

    /**
     * Represents file as appender for logs
     */
    public static final LogAppenderBuilder FileAppender = FileAppender$.MODULE$;
}
