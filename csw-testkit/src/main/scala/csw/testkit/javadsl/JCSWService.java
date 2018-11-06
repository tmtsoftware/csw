package csw.testkit.javadsl;

import csw.testkit.scaladsl.CSWService;

/**
 * Helper class for Java to get handle of [[{@link CSWService}]]
 */
@SuppressWarnings("unused")
public class JCSWService {

    public static CSWService LocationServer = CSWService.LocationServer$.MODULE$;
    public static CSWService ConfigServer   = CSWService.ConfigServer$.MODULE$;
    public static CSWService EventStore     = CSWService.EventStore$.MODULE$;
    public static CSWService AlarmStore     = CSWService.AlarmStore$.MODULE$;
}
