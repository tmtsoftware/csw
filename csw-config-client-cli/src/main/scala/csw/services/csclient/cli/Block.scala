package csw.services.csclient.cli

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object Block {
  //command line app is by nature blocking.
  //Do not use such method in library/server side code
  def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
