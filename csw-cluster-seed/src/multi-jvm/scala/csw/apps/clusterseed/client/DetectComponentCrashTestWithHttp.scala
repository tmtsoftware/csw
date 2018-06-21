package csw.apps.clusterseed.client

import csw.services.location.DetectComponentCrashTest
import csw.services.logging.scaladsl.LoggingSystemFactory

class DetectComponentCrashTestWithHttpMultiJvmNode1 extends DetectComponentCrashTestWithHttp(0, "http")
class DetectComponentCrashTestWithHttpMultiJvmNode2 extends DetectComponentCrashTestWithHttp(0, "http")
class DetectComponentCrashTestWithHttpMultiJvmNode3 extends DetectComponentCrashTestWithHttp(0, "http")

class DetectComponentCrashTestWithHttp(ignore: Int, mode: String)
    extends DetectComponentCrashTest(ignore, mode)
    with HTTPLocationService
