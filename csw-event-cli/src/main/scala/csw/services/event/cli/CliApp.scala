package csw.services.event.cli

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CliApp(commandLineRunner: CommandLineRunner) {

  def start(options: Options): Any = {
    options.op match {
      case "inspect" ⇒ await(commandLineRunner.inspect(options))
      case x         ⇒ throw new RuntimeException(s"Unknown operation: $x")
    }
  }

  // command line app is by nature blocking.
  // do not use such method in library/server side code
  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
