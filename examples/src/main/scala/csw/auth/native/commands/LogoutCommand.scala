package csw.auth.native.commands

import csw.aas.native.api.NativeAppAuthAdapter

// #logout-command
class LogoutCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter) extends AppCommand {
  override def run(): Unit = {
    nativeAppAuthAdapter.logout()
    println("SUCCESS : Logged out successfully")
  }
}
// #logout-command
