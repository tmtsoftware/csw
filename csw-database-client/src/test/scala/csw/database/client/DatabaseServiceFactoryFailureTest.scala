package csw.database.client
import akka.actor.ActorSystem
import csw.database.client.exceptions.DatabaseException
import csw.database.commons.DBTestHelper
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

//DEOPSCSW-615: DB service accessible to CSW component developers
class DatabaseServiceFactoryFailureTest extends FunSuite with Matchers with BeforeAndAfterAll {
  private val system: ActorSystem               = ActorSystem("test")
  private val postgres                          = DBTestHelper.postgres(0)
  private val dbFactory: DatabaseServiceFactory = DBTestHelper.dbServiceFactory(system)

  override def afterAll(): Unit = {
    postgres.close()
    system.terminate().futureValue
  }

  test("should throw DatabaseConnection while connecting with incorrect port") {
    intercept[DatabaseException] {
      // postgres starts on random port but while connecting it tries to look at 5432
      Await.result(dbFactory.makeDsl(), 5.seconds)
    }
  }

  test("should throw DatabaseConnection while connecting with incorrect port in Java") {
    intercept[DatabaseException] {
      // postgres starts on random port but while connecting it tries to look at 5432
      Await.result(dbFactory.jMakeDsl().toScala, 5.seconds)
    }
  }
}
