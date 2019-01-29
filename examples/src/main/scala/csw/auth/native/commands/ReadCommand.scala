package csw.auth.native.commands
import requests._

import org.backuity.clist._

class ReadCommand extends Command(name = "read", description = "reads data from server") with AppCommand {
  override def run(): Unit = {
    println(get("http://localhost:7000/data").text())
  }
}
