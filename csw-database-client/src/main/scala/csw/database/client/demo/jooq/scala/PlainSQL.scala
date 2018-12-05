package csw.database.client.demo.jooq.scala
import org.jooq.impl.DSL
import org.jooq.{Record, Result}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

object PlainSQL {
  def main(args: Array[String]): Unit = {
    val dsl = DSL.using("jdbc:postgresql://localhost:5432/postgres?user=<username>")

    // ***************************************************************** //

    val resultF = dsl
      .query("CREATE DATABASE box_office")
      .executeAsync()
      .toScala

    val result = Await.result(resultF, 5.seconds)
    println(s"CREATE DATABASE box_office - $result")

    // ***************************************************************** //

    val result1F = dsl
      .query("DROP DATABASE box_office")
      .executeAsync()
      .toScala

    val result1 = Await.result(result1F, 5.seconds)

    println(s"DROP DATABASE box_office - $result1")

    // ***************************************************************** //

    val result2F = dsl
      .query("CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL)")
      .executeAsync()
      .toScala

    val result2 = Await.result(result2F, 5.seconds)

    println(s"CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL) - $result2")

    // ***************************************************************** //

    val q1 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_1")
    val q2 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_4")
    val q3 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_2")

    import scala.concurrent.ExecutionContext.Implicits.global

    val result3F = Future {
      dsl.queries(q1, q2, q3).executeBatch()
    }

    val result3 = Await.result(result3F, 5.seconds).toList

    println(s"Multiple inserts - $result3")

    // ***************************************************************** //

    val result4F: Future[Result[Record]] = dsl
      .resultQuery("SELECT * FROM films where name = ?", "movie_1")
      .fetchAsync()
      .toScala

    val result4 = Await.result(result4F, 5.seconds)

    val list: List[Film] = result4.map(record ⇒ record.into(classOf[Film])).asScala.toList
    println(s"SELECT * FROM films where name = ? - $list")

  }
}

case class Film(id: Int, name: String)
