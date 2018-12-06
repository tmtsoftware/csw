//package csw.database.client.demo.jooq.scala
//import csw.database.client.demo.jooq.dsl_handle.DatabaseService
//import csw.database.client.demo.jooq.java.generate.Tables.{AUTHOR, BOOK}
//import csw.database.client.demo.jooq.scala.JooqExtentions.RichResultQuery
//import org.jooq._
//
//import scala.concurrent.duration.DurationDouble
//import scala.concurrent.{Await, Future}
//
//object JOOQDSL {
//  def main(args: Array[String]): Unit = {
//    val dsl = DatabaseService.defaultDSL
//
//    val query: ResultQuery[Record3[String, String, String]] = dsl
//      .select(BOOK.TITLE, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
//      .from(BOOK)
//      .join(AUTHOR)
//      .on(BOOK.AUTHOR_ID eq AUTHOR.ID)
//      .where(BOOK.PUBLISHED_IN eq 1948)
//
//    val sql = query.getSQL
//
//    println("-----------------------------")
//    println(sql)
//    println("-----------------------------")
//
//    import scala.concurrent.ExecutionContext.Implicits.global
//
//    val resultF: Future[List[BookData]] = query.fetchAsyncScala[BookData]
//
//    val bookData = Await.result(resultF, 5.seconds)
//
//    bookData.foreach(println)
//
//  }
//}
//
//case class BookData(title: String, authorFirst: String, authorLast: String)
