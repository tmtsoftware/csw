package csw.database.client.demo.jooq.scala
import csw.database.client.demo.jooq.dsl_handle.DatabaseService
import csw.database.client.demo.jooq.scala.JooqExtentions.{RichQueries, RichQuery, RichResultQuery}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

object PlainSQL {
  def main(args: Array[String]): Unit = {
    val dsl = DatabaseService.defaultDSL

    val result2F = dsl
      .query("CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL)")
      .executeAsyncScala()

    val result2 = Await.result(result2F, 5.seconds)

    println(s"CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL) - $result2")

    // ***************************************************************** //

    val q1 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_1")
    val q2 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_4")
    val q3 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_2")

    import scala.concurrent.ExecutionContext.Implicits.global

    val result3F = dsl.queries(q1, q2, q3).executeBatchAsync()
    val result3  = Await.result(result3F, 5.seconds)

    println(s"Multiple inserts - $result3")

    // ***************************************************************** //

    val result4F: Future[List[Film]] = dsl
      .resultQuery("SELECT * FROM films where name = ?", "movie_1")
      .fetchAsyncScala[Film]

    val films = Await.result(result4F, 5.seconds)

    println(s"SELECT * FROM films where name = ? - $films")

  }
}

case class Film(id: Int, name: String)
