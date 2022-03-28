/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.benchmark.time;

import com.sun.jna.Native;


// TimeLibrary is responsible for making native calls for time
public class TimeLibrary {

    static {
        Native.register("c");
    }

    public static native int clock_gettime(int clockId, TimeSpec timeSpec);


}
