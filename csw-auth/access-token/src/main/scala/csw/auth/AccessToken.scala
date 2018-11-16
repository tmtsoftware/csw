package csw.auth

import java.util.Base64

import csw.auth.Conversions._
import pdi.jwt.{JwtAlgorithm, JwtHeader, JwtJson}
import play.api.libs.json._

//todo: integrate csw logging
case class AccessToken(
    //standard checks
    sub: Option[String],
    iat: Option[Long],
    exp: Option[Long],
    iss: Option[String],
    aud: Option[String],
    jti: Option[String],
    //additional information
    given_name: Option[String],
    family_name: Option[String],
    name: Option[String],
    preferred_username: Option[String],
    email: Option[String],
    scope: Option[String],
    //auth
    realm_access: Option[RealmAccess],
    resource_access: Option[Map[String, ResourceAccess]],
    authorization: Option[Authorization]
) {
  def hasPermission(scope: String, resource: String): Boolean = {

    this.authorization match {
      case Some(auth) =>
        auth.permissions match {
          case Some(permissions) =>
            permissions.exists {
              case Permission(_, r, Some(scopes)) if r == resource => scopes.contains(scope)
              case _ => {
                System.err.println(s"user doesn't have '$scope' permission for '$resource' resource")
                false
              }
            }
          case None => {
            System.err.println("token doesn't contain permissions")
            false
          }
        }
      case None => {
        System.err.println("token doesn't authorization claim")
        false
      }
    }
  }

  def hasRole(role: String): Boolean = {

    val allRealmRoles: Set[String] = this.realm_access match {
      case Some(realmAccess) =>
        realmAccess.roles match {
          case Some(realmRoles) => realmRoles
          case None             => Set.empty
        }
      case None => Set.empty
    }

    val clientName: String = KeycloakDeployment.instance.getResourceName

    val allResourceRoles: Set[String] = this.resource_access match {
      case Some(resourceAccesses) =>
        resourceAccesses.get(clientName) match {
          case Some(resourceAccess) =>
            resourceAccess.roles match {
              case Some(roles) => roles
              case None        => Set.empty
            }
          case None => Set.empty
        }
      case None => Set.empty
    }

    allRealmRoles.union(allResourceRoles).contains(role)
  }
}

//todo: think about splitting verification and decoding
object AccessToken {

  implicit val accessTokenFormat: OFormat[AccessToken] =
    Json.format[AccessToken]

  def verifyAndDecode(token: String): Either[TokenFailure, AccessToken] = {
    getKeyId(token) match {
      case Left(error) => Left(error)
      case Right(kid) =>
        val publicKey = PublicKey.fromAuthServer(kid)
        verifyAndDecode(token, publicKey)
    }
  }

  private def getKeyId(token: String): Either[TokenFailure, String] = {

    val format = "^(.+?)\\.(.+?)\\.(.+?$)".r

    val mayBeHeaderString: Either[TokenFailure, String] = token match {
      case format(header, _, _) =>
        val jsonHeaderString = Base64.getDecoder.decode(header).map(_.toChar).mkString
        Right(jsonHeaderString)
      case _ =>
        Left(InvalidTokenFormat())
    }

    val mayBeJwtHeader: Either[TokenFailure, JwtHeader] = mayBeHeaderString match {
      case Right(headerString) => Right(JwtJson.parseHeader(headerString))
      case Left(error)         => Left(error)
    }

    mayBeJwtHeader match {
      case Left(error) => Left(error)
      case Right(jwtHeader) =>
        jwtHeader.keyId match {
          case Some(keyId) => Right(keyId)
          case None        => Left(KidMissing)
        }
    }
  }

  private def verifyAndDecode(token: String, publicKey: java.security.PublicKey): Either[TokenFailure, AccessToken] = {

    val verification: Either[TokenFailure, JsObject] =
      JwtJson.decodeJson(token, publicKey, Seq(JwtAlgorithm.RS256)).toEither.left.map(t => InvalidTokenFormat(t.getMessage))

    verification match {
      case Left(error) =>
        System.err.println(error)
        Left(error)
      case Right(jsObject) =>
        val jsResult: JsResult[AccessToken] = accessTokenFormat.reads(jsObject)
        jsResult match {
          case JsSuccess(accessToken, _) => Right(accessToken)
          case e: JsError =>
            System.err.println(allErrorMessages(e))
            Left(InvalidTokenFormat(allErrorMessages(e)))
        }
    }
  }
}
