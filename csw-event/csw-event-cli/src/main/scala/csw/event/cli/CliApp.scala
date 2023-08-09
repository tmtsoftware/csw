/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli

import org.apache.pekko.actor.typed.ActorSystem
import csw.event.cli.args.Options

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CliApp(commandLineRunner: CommandLineRunner)(implicit val system: ActorSystem[_]) {

  def start(options: Options): Any = {
    options.cmd match {
      case "inspect"   => await(commandLineRunner.inspect(options))
      case "get"       => await(commandLineRunner.get(options))
      case "publish"   => await(commandLineRunner.publish(options))
      case "subscribe" => await { val (_, doneF) = commandLineRunner.subscribe(options); doneF }
      case x           => throw new RuntimeException(s"Unknown operation: $x")
    }
  }

  // command line app is by nature blocking.
  // do not use such method in library/server side code
  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
