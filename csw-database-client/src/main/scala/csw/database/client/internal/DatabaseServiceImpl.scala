package csw.database.client.internal

import csw.database.api.scaladsl.Aliases.{SelectQuery, UpdateStatement}
import csw.database.api.scaladsl.DatabaseService
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class DatabaseServiceImpl(db: Future[Database])(implicit ec: ExecutionContext) extends DatabaseService {
  override def query[T](sql: SelectQuery[T]): Future[Seq[T]]        = db.flatMap(_.run(sql))
  override def update(sqlu: UpdateStatement): Future[Int]           = db.flatMap(_.run(sqlu))
  override def updateAll(sqlu: List[UpdateStatement]): Future[Unit] = db.flatMap(_.run(DBIO.seq(sqlu: _*)))
}
