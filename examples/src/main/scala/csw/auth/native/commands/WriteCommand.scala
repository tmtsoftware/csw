package csw.auth.native.commands

import csw.aas.native.api.NativeAppAuthAdapter
import requests._

// #write-command
class WriteCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter, value: String) extends AppCommand {
  override def run(): Unit = {
    nativeAppAuthAdapter.getAccessTokenString() match {
      case Some(token) =>
        val response =
          post(url = s"http://localhost:7000/data?value=$value", headers = Map("Authorization" -> s"Bearer $token"))

        response.statusCode match {
          case 200  => println("Success")
          case 401  => println("Authentication failed")
          case 403  => println("Permission denied")
          case code => println(s"Unrecognised error: http status code = $code")
        }

      case None =>
        println("you need to login before executing this command")
        System.exit(1)
    }
  }
}
// #write-command
