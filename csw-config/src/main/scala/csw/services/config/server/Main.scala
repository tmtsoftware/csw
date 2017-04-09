package csw.services.config.server

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

object Main {
  private val wiring = new Wiring
  import wiring._
  def main(args: Array[String]): Unit = {
    svnAdmin.initSvnRepo()
    Await.result(httpService.lazyBinding, 5.seconds)
  }
}
