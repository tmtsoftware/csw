package csw.auth.native.commands

import csw.aas.native.api.NativeAppAuthAdapter
import org.backuity.clist._

class LoginCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter)
    extends Command(name = "login", description = "performs user authentication")
    with AppCommand {
  var console: Boolean = opt[Boolean](default = false, description = "instead of using browser, prompts credentials on console")

  override def run(): Unit = {
    if (console) {
      if (nativeAppAuthAdapter.loginCommandLine())
        println("SUCCESS : Logged in successfully")
    } else {
      nativeAppAuthAdapter.login()
    }
  }
}
