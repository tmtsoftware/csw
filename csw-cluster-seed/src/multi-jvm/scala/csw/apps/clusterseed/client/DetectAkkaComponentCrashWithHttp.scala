package csw.apps.clusterseed.client

import csw.services.location.DetectAkkaComponentCrashTest

class DetectAkkaComponentCrashWithHttpMultiJvmNode1 extends DetectAkkaComponentCrashWithHttp(0, "http")
class DetectAkkaComponentCrashWithHttpMultiJvmNode2 extends DetectAkkaComponentCrashWithHttp(0, "http")
class DetectAkkaComponentCrashWithHttpMultiJvmNode3 extends DetectAkkaComponentCrashWithHttp(0, "http")

class DetectAkkaComponentCrashWithHttp(ignore: Int, mode: String)
    extends DetectAkkaComponentCrashTest(ignore, mode)
    with HTTPLocationService
