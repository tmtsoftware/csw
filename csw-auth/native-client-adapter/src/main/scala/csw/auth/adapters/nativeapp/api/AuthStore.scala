package csw.auth.adapters.nativeapp.api

trait AuthStore {

  def getAccessTokenString: Option[String]

  def getIdTokenString: Option[String]

  def getRefreshTokenString: Option[String]

  def clearStorage(): Unit

  def saveTokens(idToken: String, accessToken: String, refreshToken: String): Unit
}
