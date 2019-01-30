package csw.auth.native.commands

import csw.aas.native.api.NativeAppAuthAdapter

// #login-command
class LoginCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter) extends AppCommand {
  override def run(): Unit = {
    nativeAppAuthAdapter.login()
    println("SUCCESS : Logged in successfully")
  }
}
// #login-command
