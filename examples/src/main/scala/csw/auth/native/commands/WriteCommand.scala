package csw.auth.native.commands

import csw.aas.native.api.NativeAppAuthAdapter
import org.backuity.clist._
import requests._

class WriteCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter)
    extends Command(name = "write", description = "writes data to server")
    with AppCommand {

  var content: String = arg[String]()

  override def run(): Unit = {
    nativeAppAuthAdapter.getAccessTokenString().map { token =>
      post(url = "http://localhost:7000/data", headers = Map("Authorization" -> s"Bearer $token"))
    }
  }
}
