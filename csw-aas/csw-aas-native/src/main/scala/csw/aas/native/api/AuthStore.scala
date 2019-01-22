package csw.aas.native.api

trait AuthStore {

  /**
   * @return access token string if it is present in store
   */
  def getAccessTokenString: Option[String]

  /**
   * @return id token string if it is present in store
   */
  def getIdTokenString: Option[String]

  /**
   * @return refresh token string if it is present in store
   */
  def getRefreshTokenString: Option[String]

  /**
   * remove all the tokens from store
   * one of the use case for this is, on `logout` - clear store
   */
  def clearStorage(): Unit

  /**
   * store all tokens received from auth server as a part of login process
   * or token refresh activity
   *
   * @param idToken       token that contains user profile information (like the user's name, email, and so forth),
   *                      represented in the form of claims
   * @param accessToken   token that can be used by an application to access an API.
   *                      They inform the API that the bearer of the token has been authorized to access the API
   *                      and perform specific actions specified by the scope that has been granted.
   * @param refreshToken  token that can be used to obtain a renewed access token.
   *                      You can request new access tokens until the refresh token is expired.
   */
  def saveTokens(idToken: String, accessToken: String, refreshToken: String): Unit
}
