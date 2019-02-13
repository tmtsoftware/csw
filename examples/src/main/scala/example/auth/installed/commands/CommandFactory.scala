package example.auth.installed.commands

import akka.actor.ActorSystem
import csw.aas.installed.api.InstalledAppAuthAdapter

// #command-factory
object CommandFactory {
  def makeCommand(adapter: InstalledAppAuthAdapter,
                  args: Array[String])(implicit actorSystem: ActorSystem): Option[AppCommand] = {

    // ============ NOTE ============
    // We are doing hand parsing of command line arguments here for the demonstration purpose to keep things simple.
    // However, we strongly recommend that you use one of the existing CLI libraries. CSW makes extensive use of scopt.
    args match {
      case Array("login")          => Some(new LoginCommand(adapter))
      case Array("logout")         => Some(new LogoutCommand(adapter))
      case Array("read")           => Some(new ReadCommand)
      case Array("write", content) => Some(new WriteCommand(adapter, content))
      case _ =>
        println("invalid or no command\nvalid commands are: login, logout, read & write")
        None
    }
  }
}
// #command-factory
