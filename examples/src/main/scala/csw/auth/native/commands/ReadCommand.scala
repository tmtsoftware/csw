package csw.auth.native.commands
import requests._

// #read-command
class ReadCommand extends AppCommand {
  override def run(): Unit = {
    println(get("http://localhost:7000/data").text())
  }
}
// #read-command
