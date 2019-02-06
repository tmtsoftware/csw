package example.auth.installed.commands

import csw.aas.installed.api.InstalledAppAuthAdapter

// #logout-command
class LogoutCommand(val installedAppAuthAdapter: InstalledAppAuthAdapter) extends AppCommand {
  override def run(): Unit = {
    installedAppAuthAdapter.logout()
    println("SUCCESS : Logged out successfully")
  }
}
// #logout-command
