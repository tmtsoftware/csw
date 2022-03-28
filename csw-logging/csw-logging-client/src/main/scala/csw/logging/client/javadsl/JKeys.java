/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.javadsl;

import csw.logging.client.scaladsl.Keys$;

/**
 * Helper class for Java to get the handle of predefined keys while calling Logger api methods
 */
public class JKeys {

    /**
     * ObsId key used in logging
     */
    public static final String OBS_ID = Keys$.MODULE$.OBS_ID();
}
