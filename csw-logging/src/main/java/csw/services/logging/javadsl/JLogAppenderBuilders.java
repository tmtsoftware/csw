package csw.services.logging.javadsl;

import csw.services.logging.appenders.FileAppender$;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.appenders.StdOutAppender$;

public class JLogAppenderBuilders {
    public static final LogAppenderBuilder StdOutAppender = StdOutAppender$.MODULE$;
    public static final LogAppenderBuilder FileAppender = FileAppender$.MODULE$;
}
