package csw.auth.adapter.api

trait AuthStore {

  def getAccessTokenString: Option[String]

  def getIdTokenString: Option[String]

  def getRefreshTokenString: Option[String]

  def clearStorage(): Unit

  def saveAccessTokenResponse(idToken: String, accessToken: String, refreshToken: String): Unit
}
