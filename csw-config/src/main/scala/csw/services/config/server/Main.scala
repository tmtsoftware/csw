package csw.services.config.server

import akka.Done

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationDouble

object Main {
  private val wiring = new ServerWiring

  import wiring._

  def main(args: Array[String]): Unit = {
    svnAdmin.initSvnRepo()
    Await.result(httpService.lazyBinding, 5.seconds)
  }

  def shutdown(): Future[Done] ={
    httpService.shutdown()
  }
}
