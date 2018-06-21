package csw.apps.clusterseed.client

import csw.services.location.DetectComponentRestartTest

class DetectComponentRestartTestWithHttpMultiJvmNode1 extends DetectComponentRestartTestWithHttp(0, "http")
class DetectComponentRestartTestWithHttpMultiJvmNode2 extends DetectComponentRestartTestWithHttp(0, "http")
class DetectComponentRestartTestWithHttpMultiJvmNode3 extends DetectComponentRestartTestWithHttp(0, "http")

class DetectComponentRestartTestWithHttp(ignore: Int, mode: String)
    extends DetectComponentRestartTest(ignore, mode)
    with HTTPLocationService
