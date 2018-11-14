package csw.auth.internal
import java.nio.file.{Path, Paths}

import csw.auth.AccessToken
import csw.auth.api.AuthStore
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.{AccessTokenResponse, IDToken}

class FileAuthStore(storePath: Path) extends AuthStore {

  private val idTokenFileName      = "id_token"
  private val refreshTokenFileName = "refresh_token"
  private val accessTokenFileName  = "access_token"

  private val idTokenPath      = toPath(idTokenFileName)
  private val refreshTokenPath = toPath(refreshTokenFileName)
  private val accessTokenPath  = toPath(accessTokenFileName)

  private def toPath(name: String) = Paths.get(s"${storePath.toString}/$name")

  override def getAccessTokenString: Option[String] = read(accessTokenPath)

  override def getAccessToken(kd: KeycloakDeployment): Option[AccessToken] = getAccessTokenString.map { tokenStr ⇒
    val verifier = AdapterTokenVerifier.createVerifier(tokenStr, kd, true, classOf[AccessToken])
    verifier.verify().getToken
  }

  override def getIdTokenString: Option[String]      = read(idTokenPath)
  override def getIdToken: Option[IDToken]           = ???
  override def getRefreshTokenString: Option[String] = read(refreshTokenPath)
  override def clearStorage(): Unit                  = delete(storePath)

  override def saveAccessTokenResponse(accessTokenResponse: AccessTokenResponse, keycloakDeployment: KeycloakDeployment): Unit = {
    write(idTokenPath, accessTokenResponse.getIdToken)
    write(refreshTokenPath, accessTokenResponse.getRefreshToken)
    // todo: verify it is actually access token
    write(accessTokenPath, accessTokenResponse.getToken)
  }

  /************************************************
   * Internal APIs: Uses os-libs for file handling
  ************************************************/
  private def write(path: Path, content: String): Unit = {
    if (os.exists(path)) os.write.over(path, content)
    else os.write(path, content)
  }

  private def read(path: Path): Option[String] = if (os.exists(path)) Some(os.read(path)) else None

  private def delete(path: Path): Unit = os.remove(path)

  implicit private def toOsPath(path: Path): os.Path = os.Path(path)

}
