package csw.services.integtration.apps

object TestApp extends App {
  import org.scalatest

  scalatest.run(new TrackLocationAppIntegrationTest)
  scalatest.run(new LocationServiceIntegrationTest)
}
