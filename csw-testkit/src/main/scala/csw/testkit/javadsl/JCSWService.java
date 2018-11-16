package csw.testkit.javadsl;

import csw.testkit.scaladsl.CSWService;

/**
 * Helper class for Java to get handle of [[{@link CSWService}]]
 *
 * Specify one or more services from following ADT's while creating FrameworkTestKit
 * and testkit will make sure that those services are started.
 *
 * Example:
 * == With JUnit Integration ==
 * {{{
 *
 *   @ClassRule
 *   public static final FrameworkTestKitJunitResource testKit =
 *           new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.ConfigServer));
 *
 * }}}
 *
 * == With FrameworkTestKit ==
 * {{{
 *
 *   private static FrameworkTestKit frameworkTestKit = FrameworkTestKit.create();
 *   frameworkTestKit.start(JCSWService.ConfigServer, JCSWService.EventServer);
 *
 * }}}
 */
@SuppressWarnings("unused")
public class JCSWService {

    public static CSWService LocationServer = CSWService.LocationServer$.MODULE$;
    public static CSWService ConfigServer   = CSWService.ConfigServer$.MODULE$;
    public static CSWService EventServer    = CSWService.EventServer$.MODULE$;
    public static CSWService AlarmServer    = CSWService.AlarmServer$.MODULE$;
}
