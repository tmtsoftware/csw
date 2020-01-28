package csw.database

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.commons.DBTestHelper
import csw.database.exceptions.DatabaseException
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

//DEOPSCSW-615: DB service accessible to CSW component developers
class DatabaseServiceFactoryFailureTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  private val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  private var postgres: EmbeddedPostgres                 = _
  private var dbFactory: DatabaseServiceFactory          = _

  override def beforeAll(): Unit = {
    postgres = DBTestHelper.postgres(0)
    dbFactory = DBTestHelper.dbServiceFactory(system)
  }

  override def afterAll(): Unit = {
    postgres.close()
    system.terminate()
    system.whenTerminated.futureValue
  }

  test("should throw DatabaseConnection while connecting with incorrect port") {
    intercept[DatabaseException] {
      // postgres starts on random port but while connecting it tries to look at 5432
      Await.result(dbFactory.makeDsl(), 5.seconds)
    }
  }
}
