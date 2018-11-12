package csw.testkit.javadsl;

import csw.testkit.scaladsl.CSWService;

/**
 * Helper class for Java to get handle of [[{@link CSWService}]]
 */
@SuppressWarnings("unused")
public class JCSWService {

    public static CSWService LocationServer = CSWService.LocationServer$.MODULE$;
    public static CSWService ConfigServer   = CSWService.ConfigServer$.MODULE$;
    public static CSWService EventServer    = CSWService.EventServer$.MODULE$;
    public static CSWService AlarmServer    = CSWService.AlarmServer$.MODULE$;
}
