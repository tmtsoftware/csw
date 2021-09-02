package csw.testkit

import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.database.DatabaseServiceFactory
import csw.database.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import csw.location.api.scaladsl.RegistrationResult
import csw.network.utils.SocketUtils
import csw.testkit.database.EmbeddedPG
import csw.testkit.internal.TestKitUtils
import org.jooq.DSLContext

/**
 * DatabaseTestKit supports starting Database server using embedded postgres
 * and registering it with location service, and creating a client for the database server
 *
 * Example:
 * {{{
 *   private val testKit = DatabaseTestKit()
 *
 *   // starting postgres server
 *   // it will also register DatabaseService with location service
 *   testKit.startDatabaseService()
 *
 *   // stopping postgres server
 *   testKit.shutdownDatabaseService()
 *
 *   //creates an instance of database service factory
 *   val dbServiceFactory = testKit.databaseServiceFactory()
 *
 *   //creates a client for the particular database in database server
 *   val dsl = testKit.dslContext()
 *
 * }}}
 */
final class DatabaseTestKit private (_system: ActorSystem[?], testKitSettings: TestKitSettings) extends EmbeddedPG {
  override implicit def system: ActorSystem[?] = _system
  override implicit def timeout: Timeout       = testKitSettings.DefaultTimeout

  /**
   * Scala API to Start Database service
   *
   * It will start postgres server on provided port
   * and then register's Database service with location service
   */
  def startDatabaseService(serverPort: Int = SocketUtils.getFreePort): RegistrationResult =
    start(serverPort)

  /**
   * Shutdown Database service
   *
   * When the test has completed, make sure you shutdown Database service.
   * This will terminate actor system and postgres server.
   */
  def shutdownDatabaseService(): Unit = shutdown()

  /**
   * Create an instance of DatabaseServiceFactory
   * with given username and password
   *
   * @param dbUserName - username to be used to create the database client with the DatabaseServiceFactory
   * @param dbPassword - password to be used to create the database client with the DatabaseServiceFactory
   * @return a handle of [[csw.database.DatabaseServiceFactory]]
   */
  def databaseServiceFactory(dbUserName: String = "postgres", dbPassword: String = "postgres"): DatabaseServiceFactory =
    new DatabaseServiceFactory(system, Map(ReadUsernameHolder -> dbUserName, ReadPasswordHolder -> dbPassword))

  /**
   * Create a client for the database server of the given dbName with default username and password(both are postgres)
   *
   * @param dbName - name of the database in database server in which queries will be made
   * @return a handle of [[org.jooq.DSLContext]] to make sql query
   */
  def dslContext(dbName: String = "postgres"): DSLContext =
    TestKitUtils.await(
      databaseServiceFactory()
        .makeDsl(locationService, dbName),
      timeout
    )
}

object DatabaseTestKit {

  /**
   * Create a DatabaseTestKit
   *
   * When the test has completed you should shutdown the Database service
   * with [[DatabaseTestKit#shutdown]].
   *
   * @return handle to DatabaseTestKit which can be used to start and stop Database service
   */
  def apply(
      actorSystem: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "database-testkit"),
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): DatabaseTestKit = new DatabaseTestKit(actorSystem, testKitSettings)
}
