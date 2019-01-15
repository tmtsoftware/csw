package csw.logging.core.javadsl;

import csw.logging.core.scaladsl.Keys$;

/**
 * Helper class for Java to get the handle of predefined keys while calling Logger api methods
 */
public class JKeys {

    /**
     * ObsId key used in logging
     */
    public static final String OBS_ID = Keys$.MODULE$.OBS_ID();
}
