package csw.logging.core.javadsl;

import csw.logging.core.appenders.FileAppender$;
import csw.logging.core.appenders.LogAppenderBuilder;
import csw.logging.core.appenders.StdOutAppender$;

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
