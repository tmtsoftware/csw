package csw.services.event.cli

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class CliApp(commandLineRunner: CommandLineRunner) {
  def start(name: String, args: Array[String]): Unit =
    new ArgsParser(name).parse(args).foreach { options ⇒
      start(options)
    }

  def start(options: Options): Any = {
    options.op match {
      case "inspect" ⇒ Await.result(commandLineRunner.inspect(options), 5.seconds)
      case x         ⇒ throw new RuntimeException(s"Unknown operation: $x")
    }
  }
}
