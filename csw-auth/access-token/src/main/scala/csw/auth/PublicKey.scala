package csw.auth
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64

import org.keycloak.adapters.rotation.JWKPublicKeyLocator

import scala.io.Source

private[auth] object PublicKey {

  //todo: consider removing static state to make this more testable.
  //todo: can we use DI and inject JWKPublicKeyLocator and Keycloak?
  private val publicKeyLocator   = new JWKPublicKeyLocator()
  private val keycloakDeployment = Keycloak.deployment

  def fromString(text: String): PublicKey       = fromLines(text.split("\n"))
  def fromFile(path: String): PublicKey         = fromSource(Source.fromFile(path))
  def fromResourceFile(name: String): PublicKey = fromSource(Source.fromResource(name))
  def fromAuthServer(kid: String): PublicKey    = publicKeyLocator.getPublicKey(kid, keycloakDeployment)

  private def fromSource(source: Source): PublicKey = fromLines(source.getLines().toArray)

  private def fromLines(lines: Array[String]): PublicKey = {
    val contents = lines
      .filter(!_.startsWith("-----"))
      .mkString

    val decoded = Base64.getDecoder.decode(contents)

    KeyFactory
      .getInstance("RSA")
      .generatePublic(new X509EncodedKeySpec(decoded))
  }
}
