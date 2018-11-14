package csw.auth

import java.security.PublicKey
import java.util.Base64

import csw.auth.Conversions._
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

case class AccessToken(
    //standard checks
    sub: String,
    iat: Long,
    exp: Long,
    iss: String,
    aud: String,
    jti: String,
    //additional information
    name: String,
    permissions: Array[String],
    roles: Array[String]
)

object AccessToken {

  private implicit def accessTokenFormat: OFormat[AccessToken] =
    Json.format[AccessToken]

  def decode(token: String): Try[AccessToken] = {
    getKeyId(token) match {
      case Failure(exception) => Failure(exception)
      case Success(kid) =>
        val publicKey = PublicKey.fromAuthServer(kid)
        decode(token, publicKey)
    }
  }

  private def getKeyId(token: String): Try[String] = {

    val format = "^(.+?)\\.(.+?)\\.(.+?$)".r

    val mayBeHeaderString = token match {
      case format(header, _, _) =>
        val jsonHeaderString = Base64.getDecoder.decode(header).map(_.toChar).mkString
        Success(jsonHeaderString)
      case _ =>
        Failure(new RuntimeException("invalid token format"))
    }

    val mayBeHeader = mayBeHeaderString match {
      case Success(headerString) => Success(JwtJson.parseHeader(headerString))
      case Failure(_)            => Failure(new RuntimeException("invalid token format"))
    }

    mayBeHeader match {
      case Failure(exception) => Failure(exception)
      case Success(header) =>
        header.keyId match {
          case Some(keyId) => Success(keyId)
          case None        => Failure(new RuntimeException("token does not have a key Id"))
        }
    }
  }

  private def decode(token: String, publicKey: PublicKey): Try[AccessToken] = {

    val verification =
      JwtJson.decodeJson(token, publicKey, Seq(JwtAlgorithm.RS256))

    verification match {
      case Failure(exception) => Failure(exception)
      case Success(jsObject) =>
        val jsResult = accessTokenFormat.reads(jsObject)

        jsResult match {
          case JsSuccess(accessToken, _) => Success(accessToken)
          case e: JsError                => e
        }
    }
  }
}
