package csw.auth.native.commands
import csw.aas.native.api.NativeAppAuthAdapter

// #command-factory
object CommandFactory {
  def make(adapter: NativeAppAuthAdapter, args: Array[String]): AppCommand = {
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
