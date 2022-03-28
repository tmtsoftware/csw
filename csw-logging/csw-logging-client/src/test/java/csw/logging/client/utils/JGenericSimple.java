/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.utils;

import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JGenericLoggerFactory;

public class JGenericSimple {
    private final ILogger logger = JGenericLoggerFactory.getLogger(getClass());

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

}
