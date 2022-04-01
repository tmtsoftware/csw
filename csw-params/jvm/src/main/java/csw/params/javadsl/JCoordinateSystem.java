/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.javadsl;

import csw.params.events.CoordinateSystem;

public interface JCoordinateSystem {
    CoordinateSystem RADEC = CoordinateSystem.RADEC$.MODULE$;
    CoordinateSystem XY = CoordinateSystem.XY$.MODULE$;
    CoordinateSystem ALTAZ = CoordinateSystem.ALTAZ$.MODULE$;
}
