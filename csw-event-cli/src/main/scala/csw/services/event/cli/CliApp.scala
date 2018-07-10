package csw.services.event.cli

import akka.stream.Materializer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class CliApp(commandLineRunner: CommandLineRunner)(implicit val ec: ExecutionContext, mat: Materializer) {

  def start(options: Options): Any = {
    options.cmd match {
      case "inspect"   ⇒ await(commandLineRunner.inspect(options))
      case "get"       ⇒ await(commandLineRunner.get(options))
      case "publish"   ⇒ await(commandLineRunner.publish(options))
      case "subscribe" ⇒ await { val (_, doneF) = commandLineRunner.subscribe(options); doneF }
      case x           ⇒ throw new RuntimeException(s"Unknown operation: $x")

    }
  }

  // command line app is by nature blocking.
  // do not use such method in library/server side code
  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
