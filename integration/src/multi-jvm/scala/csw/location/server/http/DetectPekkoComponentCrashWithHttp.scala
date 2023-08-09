/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import csw.location.DetectPekkoComponentCrashTest

class DetectPekkoComponentCrashWithHttpMultiJvmNode1 extends DetectPekkoComponentCrashWithHttp(0, "http")

class DetectPekkoComponentCrashWithHttpMultiJvmNode2 extends DetectPekkoComponentCrashWithHttp(0, "http")

class DetectPekkoComponentCrashWithHttpMultiJvmNode3 extends DetectPekkoComponentCrashWithHttp(0, "http")

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
// CSW-81: Graceful removal of component
class DetectPekkoComponentCrashWithHttp(ignore: Int, mode: String)
    extends DetectPekkoComponentCrashTest(ignore, mode)
    with MultiNodeHTTPLocationService
