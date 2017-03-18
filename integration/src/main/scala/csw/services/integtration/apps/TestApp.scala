package csw.services.integtration.apps

object TestApp extends App {
  import org.scalatest

//  TODO: this app does not get terminated when this test enabled.
//  scalatest.run(new TrackLocationAppIntegrationTest)
  scalatest.run(new LocationServiceIntegrationTest)
}
