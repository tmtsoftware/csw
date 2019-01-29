package csw.auth.native.commands

import csw.aas.native.api.NativeAppAuthAdapter
import org.backuity.clist._

class LogoutCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter)
    extends Command(name = "logout", description = "logs user out")
    with AppCommand {
  override def run(): Unit = {
    nativeAppAuthAdapter.logout()
    println("SUCCESS : Logged in successfully")
  }
}
