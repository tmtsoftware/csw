package csw.auth

import java.awt.Desktop
import java.io._
import java.net.URI
import java.util.Locale
import java.util.regex.{Matcher, Pattern}

import csw.auth.api.{AuthStorage, KeycloakInstalled}
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder, ServerRequest}
import org.keycloak.common.VerificationException
import org.keycloak.common.util.KeycloakUriBuilder
import org.keycloak.representations.{AccessToken, AccessTokenResponse}
import org.keycloak.{OAuth2Constants, OAuthErrorException}
import requests.{RequestBlob, Response}

import scala.collection.mutable
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.io.{Source, StdIn}
import scala.util.{Failure, Success, Try}

private[auth] class KeycloakInstalledImpl(
    val keycloakDeployment: KeycloakDeployment,
    val store: AuthStorage,
    val locale: Locale)
    extends KeycloakInstalled {

  object Constants {
    val `WWW-Authenticate`: String = "www-authenticate"
    val maxRedirectCount: Int = 4
    val maxPasswordAttempts: Int = 3
  }

  var callbackPattern: Pattern =
    Pattern.compile("callback\\s*=\\s*\"([^\"]+)\"")
  var paramPattern: Pattern =
    Pattern.compile("param=\"([^\"]+)\"\\s+label=\"([^\"]+)\"\\s+mask=(\\S+)")
  var codePattern: Pattern = Pattern.compile("code=([^&]+)")

  def this(secretStorage: AuthStorage, locale: Locale) {
    this(KeycloakDeploymentBuilder
           .build(
             Thread
               .currentThread()
               .getContextClassLoader
               .getResourceAsStream("META-INF/keycloak.json")),
         secretStorage,
         locale)
  }

  def this(config: InputStream, secretStorage: AuthStorage, locale: Locale) {
    this(KeycloakDeploymentBuilder.build(config), secretStorage, locale)
  }

  def login(): Unit = {
    if (Desktop.isDesktopSupported) loginDesktop()
    else loginManual()
  }

  /**
    * UNUSED OVERLOAD
    *
    * @param printer
    * @param reader
    */
  private def login(printer: PrintStream, reader: Reader): Unit = {
    if (Desktop.isDesktopSupported) loginDesktop()
    else loginManual(printer, reader)
  }

  def logout(): Unit = {
    if (store.getStatus.isDefined && store.getStatus.get == Status.LOGGED_DESKTOP)
      logoutDesktop()

    store.clearStorage()
  }

  def loginDesktop(): Unit = {
    val callback = new CallbackListener(keycloakDeployment)
    callback.start()

    val redirectUri = "http://localhost:" + callback.server.getLocalPort
    import java.util.UUID
    val state = UUID.randomUUID.toString

    val builder: KeycloakUriBuilder = keycloakDeployment.getAuthUrl
      .clone()
      .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
      .queryParam(OAuth2Constants.CLIENT_ID, keycloakDeployment.getResourceName)
      .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
      .queryParam(OAuth2Constants.STATE, state)
      .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

    if (locale != null) {
      builder.queryParam(OAuth2Constants.UI_LOCALES_PARAM, locale.getLanguage)
    }

    val authUrl: String = builder.build().toString

    Desktop.getDesktop.browse(new URI(authUrl))

    callback.join()

    if (!state.equals(callback.state))
      throw new VerificationException("Invalid state")

    if (callback.error != null)
      throw new OAuthErrorException(callback.error, callback.errorDescription)

    if (callback.errorException != null) throw callback.errorException

    processCode(callback.code, redirectUri)

    store.saveStatus(Status.LOGGED_DESKTOP)
  }

  def loginManual(): Unit = {
    loginManual(System.out, new InputStreamReader(System.in))
  }


  private def loginManual(printer: PrintStream, reader: Reader): Unit = {
    val redirectUri = "urn:ietf:wg:oauth:2.0:oob"

    val authUrl = keycloakDeployment.getAuthUrl
      .clone()
      .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
      .queryParam(OAuth2Constants.CLIENT_ID, keycloakDeployment.getResourceName)
      .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
      .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
      .build()
      .toString

    printer.println(
      "Open the following URL in a browser. After login copy/paste the code back and press <enter>")
    printer.println(authUrl)
    printer.println()
    printer.print("Code: ")

    val code = readCode(reader)
    processCode(code, redirectUri)

    store.saveStatus(Status.LOGGED_MANUAL)
  }

  def loginCommandLine(
      redirectUri: String = "urn:ietf:wg:oauth:2.0:oob"): Boolean = {
    val authUrl: String = keycloakDeployment.getAuthUrl
      .clone()
      .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
      .queryParam(OAuth2Constants.CLIENT_ID, keycloakDeployment.getResourceName)
      .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
      .queryParam("display", "console")
      .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
      .build()
      .toString

    val response: Response = requests.get(authUrl)
    loginCommandLine(response, redirectUri, 0, 0)
  }

  private implicit class OptionOps[A](opt: Option[A]) {

    def toTry(msg: String): Try[A] = {
      opt
        .map(Success(_))
        .getOrElse(Failure(new RuntimeException(msg)))
    }
  }

  def getAccessTokenString(
      minValidity: FiniteDuration = 0.seconds): Try[String] = {

    store.getAccessToken
      .flatMap { at =>
        val exp = at.getExpiration
          .asInstanceOf[Long] * 1000 - minValidity.toMillis
        if (exp < System.currentTimeMillis) refreshAccessToken()
        store.getAccessTokenString
      }
      .toTry("Access token not found")
  }

  def getAccessToken(
      minValidity: FiniteDuration = 0.seconds): Try[AccessToken] = {

    store.getAccessToken
      .flatMap(at => {
        val exp = at.getExpiration
        if (exp < System.currentTimeMillis) refreshAccessToken()
        store.getAccessToken
      })
      .toTry("Access token not found")
  }

  private def refreshAccessToken(): Unit = {
    val tokenResponse =
      ServerRequest.invokeRefresh(keycloakDeployment,
                                  store.getRefreshTokenString.get)
    store.saveAccessTokenResponse(tokenResponse, keycloakDeployment)
  }

  private def loginCommandLine(response: Response,
                               redirectUri: String,
                               redirectCount: Int,
                               invalidCount: Int): Boolean = {
    response.statusCode match {
      case 403 => handle403(response)
      case 401 => handle401(response, redirectCount, redirectUri, invalidCount)
      case 302 => handle302(response, redirectCount, redirectUri, invalidCount)
      case _ =>
        System.err.println(
          "Unknown response from server: " + response.statusCode)
        false
    }
  }

  private def handle401(response: Response,redirectCount:Int, redirectUri:String, invalidCount:Int):Boolean ={
    val authenticationHeader: Option[String] =
      response.headers.get(Constants.`WWW-Authenticate`) match {
        case Some(Seq(head))    => Some(head)
        case Some(Seq(head, _)) => Some(head)
        case _                  => None
      }

    if (authenticationHeader.isEmpty) {
      System.err.println(
        "Failure: Invalid protocol. No 'www-authenticate' header")
      return false
    }
    if (!authenticationHeader.get.contains("X-Text-Form-Challenge")) {
      System.err.println("Failure: Invalid WWW-Authenticate header.")
      return false
    }

    if (response.contentType.isDefined) {
      invalidCount match {
        case i if i > Constants.maxPasswordAttempts =>
          System.err.println("Invalid credentials")
          return false
        case i if i > 0 => println("Invalid credentials. Please try again")
        case _          => println(Source.fromResource("TMT-Logo.txt").mkString)
      }
    }

    var m: Matcher = callbackPattern.matcher(authenticationHeader.get)
    if (!m.find) {
      System.err.println("Failure: Invalid WWW-Authenticate header.")
      return false
    }
    val callback: String = m.group(1)
    m = paramPattern.matcher(authenticationHeader.get)

    val form: mutable.Map[String, String] = mutable.Map.empty

    while (m.find()) {
      val param = m.group(1)
      val label = m.group(2)
      val mask = m.group(3).trim
      val maskInput = mask.equals("true")

      var value: String = null
      if (maskInput) {
        //todo:fix this security issue. password should not be displayed on console
        val txt = StdIn.readLine(label)
        value = new String(txt)
      } else value = StdIn.readLine(label)
      form += param -> value
    }

    val nextResponse = requests.post(
      url = callback,
      data = RequestBlob.FormEncodedRequestBlob(form),
      maxRedirects = 0
    )

    loginCommandLine(nextResponse,
      redirectUri,
      redirectCount,
      invalidCount + 1)
  }

  private def handle302(response: Response, redirectCount:Int, redirectUri:String, invalidCount:Int): Boolean ={
    if (redirectCount > Constants.maxRedirectCount) {
      println("too many redirects")
      return false
    }

    val location: String = response.location.getOrElse {
      println("ERROR: there is no redirect url specified")
      ""
    }
    val m = codePattern.matcher(location)

    if (!m.find) {
      loginCommandLine(requests.get(location),
        redirectUri,
        redirectCount + 1,
        invalidCount)
    } else {
      val code = m.group(1)
      processCode(code, redirectUri)
      true
    }
  }

  private def handle403(response: Response) = {
    if (response.contentType.nonEmpty) {
      System.err.println(response.text())
    } else {
      System.err.println("Forbidden to login")
    }
    false
  }
  private def processCode(code: String, redirectUri: String): Unit = {
    val tokenResponse: AccessTokenResponse = ServerRequest
      .invokeAccessCodeToToken(keycloakDeployment, code, redirectUri, null)
    store.saveAccessTokenResponse(tokenResponse, keycloakDeployment)
  }

  private def readCode(
      reader: Reader,
      characterBuffer: Array[Char] = Array[Char](1),
      sb: mutable.StringBuilder = new StringBuilder): String = {
    reader.read(characterBuffer) match {
      case -1 => sb.toString
      case _ =>
        val c: Char = characterBuffer(0)
        c match {
          case ' ' | '\n' | '\r' => sb.toString()
          case _ =>
            sb.append(c)
            readCode(reader, characterBuffer, sb)
        }
    }
  }

  private def logoutDesktop(): Unit = {
    val callback = new CallbackListener(keycloakDeployment)
    callback.start()

    val redirectUri = "http://localhost:" + callback.server.getLocalPort

    val logoutUrl = keycloakDeployment.getLogoutUrl
      .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
      .build()
      .toString

    Desktop.getDesktop.browse(new URI(logoutUrl))

    callback.join()

    if (callback.errorException != null) throw callback.errorException
  }
}
