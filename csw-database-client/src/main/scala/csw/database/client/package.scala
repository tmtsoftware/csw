package csw.database

/**
 * == Database Service ==
 *
 * This module provides the client implementation of data access stored in postgres server. To access the database service,
 * it is expected that developers create an instance of `DatabaseServiceFactory`.
 *
 * === Example: DatabaseServiceFactory ===
 *
 * {{{
 *
 *      val system = ActorSystem("test") // in component handlers the ActorSystem should be provided from ActorContext e.g. ctx.system
 *      val dbFactory = new DatabaseServiceFactory(system)
 *      val dsl: DSLContext = dbFactory.makeDsl(locationService, "postgres") // postgres is a dbName here
 *
 * }}}
 *
 * There are overloads of `makeDsl()` available. Once the `DSLContext` is created, it can be used to access data from postgres.
 *
 * Complete guide of different flavours of `makeDsl()` and how to use `DSLContext` to access data is available at:
 * [[https://tmtsoftware.github.io/csw/services/database.html]]
 *
 */
package object client {}
