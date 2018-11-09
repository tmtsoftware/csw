package csw.auth
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64

import com.typesafe.config.Config
import org.keycloak.adapters.rotation.JWKPublicKeyLocator

import scala.io.Source

/**
 * This object is should be used when not fetching public keys from auth server
 * but instead want to use hardcoded public keys
 */
object PublicKey {

  val publicKeyLocator = new JWKPublicKeyLocator()

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

  def fromAuthServer(kid: String, config: Config): PublicKey = {
    val kd = KeycloakDeploymentFactory.createInstance(config)
    publicKeyLocator.getPublicKey(kid, kd)
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
