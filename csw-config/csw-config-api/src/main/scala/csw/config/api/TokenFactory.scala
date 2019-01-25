package csw.config.api

/**
  * [[csw.config.api.scaladsl.ConfigService]] deals with some admin functionality.
  * In order to perform those, one need to implement this interface.
  *
  * Refer `CliTokenFactory` class from `csw-config-cli` project which implements this interface.
  */
trait TokenFactory {

  /**
    * This token is used by an [[csw.config.api.scaladsl.ConfigService]] to access an admin API.
    * This inform the API that the bearer of the token has been authorized to access the API and
    * perform specific actions specified by the scope that has been granted.
    *
    * Refer `csw-aas` project and their corresponding documentation to know more about
    * how one can authenticate and authorize with auth server and get this token.
    *
    * @return raw token string which can then be used to access admin api
    */
  def getToken: String

}
