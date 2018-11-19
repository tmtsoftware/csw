package csw.auth

private[auth] object Example extends App {

  val accessToken: Either[TokenVerificationFailure, AccessToken] = AccessToken.verifyAndDecode(
    "header.payload.signature"
  )

  accessToken match {
    case Left(failure) =>
      System.err.println(failure)
      sys.exit(1)
    case Right(at) =>
      println(s"access token: $at")
  }
}
