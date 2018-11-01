package csw.testkit.javadsl;

import csw.testkit.scaladsl.Service;

/**
 * Helper class for Java to get handle of [[{@link Service}]]
 */
@SuppressWarnings("unused")
public class JService {

    public static Service Location = Service.LocationServer$.MODULE$;
    public static Service Config = Service.ConfigServer$.MODULE$;
    public static Service Event  = Service.EventStore$.MODULE$;
    public static Service Alarm  = Service.AlarmStore$.MODULE$;
}
