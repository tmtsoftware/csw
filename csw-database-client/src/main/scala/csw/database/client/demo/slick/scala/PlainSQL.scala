package csw.database.client.demo.slick.scala
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

object PlainSQL extends App {
  val db: PostgresProfile.backend.Database =
    Database.forURL("jdbc:postgresql://localhost:5432/postgres?user=salonivithalani")

  private val createQuery = sqlu"CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL)"
  Await.result(db.run(createQuery), 5.seconds)

  private val inserts: DBIOAction[Unit, NoStream, Effect.Write] = DBIO.seq(
    sqlu"INSERT INTO films(name) VALUES ('movie_1')",
    sqlu"INSERT INTO films(name) VALUES ('movie_4')",
    sqlu"INSERT INTO films(name) VALUES ('movie_2')"
  )
  Await.result(db.run(inserts), 5.seconds)

  val name                = "movie_1"
  private val selectQuery = sql"SELECT * FROM films where name = $name".as[(Int, String)]
  Await.result(db.run(selectQuery), 5.seconds)

}
