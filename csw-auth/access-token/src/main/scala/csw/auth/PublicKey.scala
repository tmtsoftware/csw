package csw.auth
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64

import org.keycloak.adapters.rotation.JWKPublicKeyLocator

import scala.io.Source

private[auth] object PublicKey {

  private val publicKeyLocator   = new JWKPublicKeyLocator()
  private val keycloakDeployment = KeycloakDeployment.instance

  def fromString(text: String): PublicKey = {
    val lines = text.split("\n")
    fromLines(lines)
  }

  def fromFile(path: String): PublicKey = {
    fromSource(Source.fromFile(path))
  }

  def fromResourceFile(name: String): PublicKey = {
    fromSource(Source.fromResource(name))
  }

  def fromAuthServer(kid: String): PublicKey = {
    publicKeyLocator.getPublicKey(kid, keycloakDeployment)
  }

  private def fromSource(source: Source): PublicKey = {
    val lines = source.getLines().toArray
    fromLines(lines)
  }

  private def fromLines(lines: Array[String]): PublicKey = {
    val contents = lines
      .filter(!_.startsWith("-----"))
      .mkString

    val decoded = Base64.getDecoder.decode(contents)

    val publicKey: PublicKey = KeyFactory
      .getInstance("RSA")
      .generatePublic(new X509EncodedKeySpec(decoded))

    publicKey
  }
}
