import java.util.concurrent.TimeUnit

import akka.util.Timeout
import tmt.development.dsl.Dsl.wiring._

val timeout = new Timeout(5, TimeUnit.SECONDS)

cs.track("assembly1-assembly-akka")

forEach { command =>
  if (command.commandName == "setup-assembly1") {
    println(cs.submit("assembly1", command, timeout))
  }
}
