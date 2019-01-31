package csw.auth.native.commands
import akka.actor.ActorSystem
import csw.aas.native.api.NativeAppAuthAdapter

// #command-factory
object CommandFactory {
  def makeCommand(adapter: NativeAppAuthAdapter, args: Array[String])(implicit actorSystem: ActorSystem): AppCommand = {
    args match {
      case Array("login")          => new LoginCommand(adapter)
      case Array("logout")         => new LogoutCommand(adapter)
      case Array("read")           => new ReadCommand
      case Array("write", content) => new WriteCommand(adapter, content)
      case _ =>
        println("invalid or no command\nvalid commands are: login, logout, read & write")
        System.exit(1)
        null
    }
  }
}
// #command-factory
