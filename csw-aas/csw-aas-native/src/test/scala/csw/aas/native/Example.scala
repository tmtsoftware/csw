package csw.aas.native

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.aas.native.scaladsl.FileAuthStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.duration.DurationLong

object Example extends App {

  private implicit val actorSystem: ActorSystem             = ActorSystem()
  private implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  import actorSystem._

  val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  val nativeAuthAdapter = NativeAppAuthAdapterFactory.make(locationService, new FileAuthStore(Paths.get("/tmp/auth")))

  println("login initiated")
  nativeAuthAdapter.login()

  private val expires: Long = nativeAuthAdapter.getAccessToken().get.exp.get
  println(s"Expiring on: $expires")
  println(System.currentTimeMillis() / 1000)

  private val timeLeft: Long = expires - System.currentTimeMillis() / 1000
  println(s"time left to expire: $timeLeft")

  println(nativeAuthAdapter.getAccessTokenString())

  println(nativeAuthAdapter.getAccessTokenString((timeLeft + 100).seconds))
}
