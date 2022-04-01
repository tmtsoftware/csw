/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.javadsl;

import csw.params.events.OperationalState;

public interface JOperationalState {
    OperationalState READY = OperationalState.READY$.MODULE$;
    OperationalState ERROR = OperationalState.ERROR$.MODULE$;
    OperationalState BUSY = OperationalState.BUSY$.MODULE$;
    OperationalState NOT_READY = OperationalState.NOT_READY$.MODULE$;
}
