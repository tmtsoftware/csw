package example.auth.installed.commands

import csw.aas.installed.api.InstalledAppAuthAdapter

// #login-command
class LoginCommand(val installedAppAuthAdapter: InstalledAppAuthAdapter) extends AppCommand {
  override def run(): Unit = {
    installedAppAuthAdapter.login()
    println("SUCCESS : Logged in successfully")
  }
}
// #login-command
