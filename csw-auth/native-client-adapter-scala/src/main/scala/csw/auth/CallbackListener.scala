package csw.auth

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.{ServerSocket, Socket}

import org.keycloak.OAuth2Constants
import org.keycloak.adapters.KeycloakDeployment

private[auth] class CallbackListener(
    val keycloakDeployment: KeycloakDeployment)
    extends Thread {

  val server: ServerSocket = new ServerSocket(0)

  var code, error, errorDescription, state: String = _
  var errorException: IOException = _
  var socket: Socket = _

  override def run(): Unit = {
    try {
      socket = server.accept()

      val br = new BufferedReader(new InputStreamReader(socket.getInputStream))
      val request = br.readLine
      var url = request.split(" ")(1)

      if (url.indexOf('?') >= 0) {
        url = url.split("\\?")(1)
        val params = url.split("&")
        for (param <- params) {
          val p = param.split("=")
          if (p(0) == OAuth2Constants.CODE) code = p(1)
          else if (p(0) == OAuth2Constants.ERROR) error = p(1)
          else if (p(0) == "error-description") errorDescription = p(1)
          else if (p(0) == OAuth2Constants.STATE) state = p(1)
        }
      }

      import java.io.OutputStreamWriter
      val out = new OutputStreamWriter(socket.getOutputStream)
      import java.io.PrintWriter
      val pw = new PrintWriter(out)

      if (error == null) {
        pw.println("HTTP/1.1 302 Found")
        pw.println(
          "Location: " + keycloakDeployment.getTokenUrl.replace("/token",
                                                                "/delegated"))
      } else {
        pw.println("HTTP/1.1 302 Found")
        pw.println(
          "Location: " + keycloakDeployment.getTokenUrl
            .replace("/token", "/delegated?error=true"))
      }
      pw.flush()
      socket.close()
    } catch {
      case e: IOException => errorException = e
    }

    try server.close()
    catch {
      case e: IOException =>
        println("ERROR")
        println(e)
    }
  }
}
