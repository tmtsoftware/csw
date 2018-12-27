# Database Service

The Database Service provides API to manage database connections and access data in the TMT software system. The service expects
`Postgres` as database server. It uses `Jooq` library underneath to manage database access, connection pooling, etc.
To describe `JOOQ` briefly, it is a java library that provides api for accessing data i.e. DDL support, DML support, fetch,
batch execution, prepared statements, etc. safety against sql injection connection pooling, etc. To know more about JOOQ and
it's features please refer this [link](https://www.jooq.org/learn/).

<!-- introduction to the service -->

## Dependencies

To use the Database service, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-database-client" % "$version$"
    ```
    @@@
    
## Database Service Factory

Database service requires `postgres` server to be running on the machine. To start the postgres server for development and testing
purposes, refer @ref:[Starting apps for development](../commons/apps.md#starting-apps-for-development).

Once the postgres is up and running, database service can be used to connect and access the data. It is assumed that there
will be more than one user types registered with postgres i.e. for read access, for write access, for admin access, etc.

By default while connecting to postgres, database service will provide read access for data. To achieve that, create an 
instance of `DatabaseServiceFactory` and use it as shown below:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-access }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dbFactory-access }
 
 
`makeDsl`/`jMakeDsl` takes `locationService` to locate the postgres server running and connect to it. It connects to the
database by the provided `dbName`. It picks username and password for read access profile from environment variables 
i.e. it looks at `dbReadUsername` for username and `dbReadPassword` for password, hence it is expected from developers
to set these environment variables prior to using `DatabaseServiceFactory`. 

`makeDsl`/`jMakeDsl` returns a `JOOQ` type `DSLContext`. DSLContext provides mechanism to access the data stored in postgres
using JDBC driver underneath. The usage of DSLContext in component development will be explained in later sections.  

@@@note 

* Any exception encountered while connecting to postgres server will be wrapped in `DatabaseException`.

@@@

### Connect for write access

In order to connect to postgres for write access (or any other access other than read), use the `DatabaseServiceFactory`
as shown below: 

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-write-access }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dbFactory-write-access }

Here the username is picked from `dbWriteUsername` and password is picked from `dbWritePassword` environment variables. Hence, it is
expected from developers to set environment variables prior to using these method. 
 
### Connect for development or testing

For development and testing purposes, all database connection properties can be provided from `application.conf` including
username and password. This will not require to set environment variables for credentials as described in previous section.
In order to do so, use the `DatabaseServiceFactory` as shown below:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-test-access }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dbFactory-test-access }
  
The reference for providing database properties is shown below:

reference.conf
:   @@snip [reference.conf](../../../../csw-database-client/src/main/resources/reference.conf)

In order to override any property shown above, it needs to be defined in `application.conf` for e.g. a sample application.conf
can look as follows:

```
csw-database.hikari-datasource.dataSource {
  serverName = localhost
  portNumber = 5432
  databaseName = postgres
  user = postgres
  password = postgres
}
```    

@@@note

By default csw configures `HikariCP` connection pool for managing connections with postgres server. To know more about `HikariCP`
please refer this [link](http://brettwooldridge.github.io/HikariCP/). 

@@@  

## Using DSLContext

Once the DSLContext is returned from `makeDsl/jMakeDsl`, it can be used to provide plain SQL to database service and 
get it executed on postgres server. 

### CREATE

To create a table, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-create }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-create }

### INSERT

To insert data in batch, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-batch }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-batch }

@@@note

* The insert statements above gets mapped to prepared statements underneath at jdbc layer and values like `movie_1`,
 `movie_2` and `2` from the example are bound to the dynamic parameters of these generated prepared statements.
* As prepared statements provide safety against SQL injection, it is recommended to use prepared statements instead of static
 SQL statements whenever there is a need to dynamically bind values.
* In the above example, two insert statements are batched together and sent to postgres server in a single call. 
 `executeBatchAsync/executeBatch` maps to batch statements underneath at jdbc layer.

@@@

### SELECT

To select data from table, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-fetch }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-fetch }

### STORED FUNCTION

To create a stored function, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-function }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-function }

Similarly, any SQL queries can be written with the help of DSLContext including stored procedures.

@@@note

* If there is a syntax error in SQL queries the `Future/CompletableFuture` returned will fail with `CompletionException` and 
 `CompletionStage` will fail with `ExecutionException`. But both `CompletionException` and `ExecutionException` will have 
 Jooq's `DataAccessException` underneath as cause. 

@@@

## Source code for examples

* @github[Scala Example](/examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala)
* @github[Java Example](/examples/src/main/java/csw/database/JAssemblyComponentHandlers.java)

