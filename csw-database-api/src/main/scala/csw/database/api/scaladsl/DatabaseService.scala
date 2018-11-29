package csw.database.api.scaladsl

import slick.jdbc.GetResult

import scala.concurrent.Future

trait DatabaseService {
  def executeQuery[T](query: String)(implicit getResult: GetResult[T]): Future[Seq[T]]
  def execute(query: String): Future[Int]
  def execute(queries: List[String]): Future[Unit]
}
