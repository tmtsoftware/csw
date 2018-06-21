package csw.apps.clusterseed.client

import csw.services.location.TrackLocationTest

class TrackLocationTestWithHttpMultiJvmNode1 extends TrackLocationTestWithHttp(0)
class TrackLocationTestWithHttpMultiJvmNode2 extends TrackLocationTestWithHttp(0)
class TrackLocationTestWithHttpMultiJvmNode3 extends TrackLocationTestWithHttp(0)

class TrackLocationTestWithHttp(ignore: Int) extends TrackLocationTest(0, "http") with HTTPLocationService
