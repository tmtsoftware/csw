package demo

import slick.dbio.Effect
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object SlickDSLExample extends App {

  import Schema._

  val db: PostgresProfile.backend.Database = Database.forURL("jdbc:postgresql://localhost:5432/bharats?user=bharats")

  val schema = suppliers.schema ++ coffees.schema

  def createTables(): FixedSqlAction[Unit, NoStream, Effect.Schema] = {
    // Create the tables, including primary and foreign keys
    schema.create
  }

  def dropTables(): Future[Unit] = {
    // Create the tables, including primary and foreign keys
    db.run(schema.drop)
  }

  def insertIntoSuppliers(): FixedSqlAction[Option[Int], NoStream, Effect.Write] = {
    // Insert some suppliers
    suppliers ++= Seq(
      (101, "Acme, Inc.", "99 Market Street", "Groundsville", "CA", "95199"),
      (49, "Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460"),
      (150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966")
    )
    // Equivalent SQL code:
    // insert into SUPPLIERS(SUP_ID, SUP_NAME, STREET, CITY, STATE, ZIP) values (?,?,?,?,?,?)
  }

  def insertIntoCoffees(): FixedSqlAction[Option[Int], NoStream, Effect.Write] = {
    // Insert some coffees (using JDBC's batch insert feature, if supported by the DB)
    coffees ++= Seq(
      ("Colombian", 101, 7.99, 0, 0),
      ("French_Roast", 49, 8.99, 0, 0),
      ("Espresso", 150, 9.99, 0, 0),
      ("Colombian_Decaf", 101, 8.99, 0, 0),
      ("French_Roast_Decaf", 49, 9.99, 0, 0)
    )
    // Equivalent SQL code:
    // insert into COFFEES(COF_NAME, SUP_ID, PRICE, SALES, TOTAL) values (?,?,?,?,?)
  }

  def updateCoffees(): Future[Int] = {
    println("updating:")
    println("*" * 20)
    val q = for { c <- coffees if c.name === "Espresso" } yield c.price
    db.run(q.update(10.49))
  }

  def deleteCoffees(): Future[Int] = {
    println("deleting:")
    println("*" * 20)
    val q = coffees.filter(_.supID === 101)
    db.run(q.delete)
  }

  def queryCoffees: Future[Unit] = {
    // Read all coffees and print them to the console
    println("Coffees:")
    println("*" * 20)
    db.run(coffees.result).map(_.foreach(println))
    // Equivalent SQL code:
    // select COF_NAME, SUP_ID, PRICE, SALES, TOTAL from COFFEES
  }

  def joinQueryOnSupplierAndCoffees = {
    // Perform a join to retrieve coffee names and supplier names for
    // all coffees costing less than $9.00
    println("Performing Joins:")
    println("*" * 20)
    val q2 = for {
      c <- coffees if c.price < 9.0
      s <- suppliers if s.id === c.supID
    } yield (c.name, s.name)
    // Equivalent SQL code:
    // select c.COF_NAME, s.SUP_NAME from COFFEES c, SUPPLIERS s where c.PRICE < 9.0 and s.SUP_ID = c.SUP_ID
    db.run(q2.result)
      .map(_.foreach(t => println(t._1 + " supplied by " + t._2)))
  }

  try {
    val setup: Future[Unit] =
      db.run(
        DBIO.seq(
          createTables(),
          insertIntoSuppliers(),
          insertIntoCoffees(),
        )
      )

    Await.result(setup, Duration.Inf)
    Await.result(queryCoffees, Duration.Inf)

    Await.result(deleteCoffees(), Duration.Inf)
    Await.result(queryCoffees, Duration.Inf)

    Await.result(updateCoffees(), Duration.Inf)
    Await.result(queryCoffees, Duration.Inf)

    Await.result(joinQueryOnSupplierAndCoffees, Duration.Inf)
    Await.result(dropTables(), Duration.Inf)

  } finally db.close
}
