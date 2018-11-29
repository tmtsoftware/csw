package csw.database.client.internal

import csw.database.api.scaladsl.DatabaseService
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, SQLActionBuilder, SetParameter}

import scala.concurrent.{ExecutionContext, Future}

class DatabaseServiceImpl(db: Future[Database])(implicit ec: ExecutionContext) extends DatabaseService {

  override def executeQuery[T](query: String)(implicit getResult: GetResult[T]): Future[Seq[T]] =
    db.flatMap(_.run(SQLActionBuilder(query, SetParameter.SetUnit).as[T]))

  override def execute(query: String): Future[Int] =
    db.flatMap(_.run(SQLActionBuilder(query, SetParameter.SetUnit).asUpdate))

  override def execute(queries: List[String]): Future[Unit] = {
    val queryActions: List[DBIO[Int]] =
      queries.map(SQLActionBuilder(_, SetParameter.SetUnit).asUpdate)
    db.flatMap(_.run(DBIO.seq(queryActions: _*)))
  }
}
