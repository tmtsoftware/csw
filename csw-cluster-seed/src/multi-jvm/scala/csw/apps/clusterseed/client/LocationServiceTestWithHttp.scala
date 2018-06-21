package csw.apps.clusterseed.client

import csw.services.location.LocationServiceTest

class LocationServiceTestWithHttpMultiJvmNode1 extends LocationServiceTestWithHttp(0, "http")
class LocationServiceTestWithHttpMultiJvmNode2 extends LocationServiceTestWithHttp(0, "http")

class LocationServiceTestWithHttp(ignore: Int, mode: String) extends LocationServiceTest(ignore, mode) with HTTPLocationService
