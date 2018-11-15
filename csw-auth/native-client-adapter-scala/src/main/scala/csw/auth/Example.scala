package csw.auth

import java.nio.file.Paths

import csw.auth.adapter.NativeAuthServiceFactory
import csw.auth.adapter.internal.{FileAuthStore, NativeAuthServiceImpl}

import scala.concurrent.duration.DurationDouble

object Example extends App {
  val keycloak = NativeAuthServiceFactory.make(new FileAuthStore(Paths.get("/tmp/auth")))

  println("login initiated")
//  keycloak.login()

  private val expires: Int = keycloak.asInstanceOf[NativeAuthServiceImpl].getAccessToken().map(x ⇒ x.getExpiration).get
  println(s"Expiring on: $expires")
  println(System.currentTimeMillis() / 1000)

  private val timeLeft: Long = expires - System.currentTimeMillis() / 1000
  println(s"time left to expire: $timeLeft")

  println(keycloak.getAccessTokenString())

  println(keycloak.getAccessTokenString((timeLeft + 100).seconds))

  Thread.sleep(500000)

}
