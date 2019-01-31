# Database Service

The Database Service is included in TMT Common Software for use by components that need the features of a relational database.
CSW Database Service provides a TMT-standard relational database and connection library. Databases created by Database Service
will be stored reliably at the site during operations.

The Database Service provides an API to manage database connections and access data in the TMT Software System. The service provides
`PostgreSQL` as the underlying database server. It uses the `Jooq` library underneath to manage database access, connection pooling, etc.

@@@ note
`Jooq` is a Java library that provides a higher level API for accessing data i.e. DDL support, DML support, fetch,
batch execution, prepared statements, safety against sql injection, connection pooling, etc. To know more about Jooq and
its features, please refer to this [link](https://www.jooq.org/learn/).
@@@

Database Service requires `PostgreSQL` server to be running on a machine. To start the PostgreSQL server for development 
and testing purposes, refer to @ref:[Starting Apps for Development](../commons/apps.md#starting-apps-for-development).

Once the PostgreSQL is up and running, Database Service can be used to connect and access data. It is assumed that there
will be more than one user types registered with postgres i.e. for read access, for write access, for admin access, etc.

<!-- introduction to the service -->

## Dependencies

To include the Database Service in a component, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-database" % "$version$"
    ```
    @@@

## Accessing Database Service
Database Service is accessed differently than other CSW services in that it is not passed to a component through 
`CswContext/JCswContext` in the component's ComponentHandlers. To access Database Service, developers create a 
`DatabaseServiceFactory`. DatabaseServiceFactory can be created anywhere in the code using
an `ActorSystem` and its creation is explained in next section. 

@@@ note
   
Creating a new DatabaseServiceFactory does not mean a new connection to PostgreSQL server will be created. Hence, creating
multiple DatabaseServiceFactory per component can be considered pretty cheap and harmless.

@@@

#### Connect for Read Access

Our access approach is that all components can read any Database Service database and clients that only
need read access use the following factory method. But a writer will need a special username/password with write access as shown below.

By default while connecting to PostgreSQL, Database Service will provide read access for data. 
To achieve that, create an instance of `DatabaseServiceFactory` and use it as shown below:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-access }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dbFactory-access }
 
The underlying database server is registered with the Location Service.
`makeDsl`/`jMakeDsl` takes `locationService` to locate the PostgreSQL server running and connect to it. It connects to the
database by the provided `dbName`. It picks the database username and password for read access profile from 
TMT-standard environment variables called `dbReadUsername` for username and `dbReadPassword` for password, hence it is expected that developers
will set these environment variables prior to using `DatabaseServiceFactory`. PostgreSQL should also be initialized with a read-only
user and password that agrees with the values in the environment variables. This approach is used to keep from putting
database login information in the source code. 

@@@ note
Eventually, all TMT user logins will all have these environment variables set with the agreed upon read-only user and password.    
@@@

`makeDsl`/`jMakeDsl` returns a `Jooq` type `DSLContext`. DSLContext provides the mechanism to access the data stored in PostgreSQL
using the selected JDBC driver underneath. The usage of DSLContext in component development will be explained in later sections.  

@@@ note { title=Hint }

* Any exception encountered while connecting to postgres server will be wrapped in `DatabaseException`.

@@@

#### Connect for Write Access

In order to connect to PostgreSQL for write access (or any other access other than read), use the `DatabaseServiceFactory`
as shown below with different environment variables: 

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-write-access }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dbFactory-write-access }

Here the username is picked from `dbWriteUsername` and password is picked from `dbWritePassword` environment variables. Hence, it is
expected from developers to set environment variables prior to using this method with the user name and password to use for
write access. 
 
#### Connect for Development or Testing

For development and testing purposes, all database connection properties can be provided from `application.conf` including
username and password. This will not require setting any environment variables for credentials as described in previous sections.
In order to do so, use the `DatabaseServiceFactory` as shown below:
 
Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-test-access }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dbFactory-test-access }
  
The reference for providing database properties is shown below:

reference.conf
:   @@snip [reference.conf](../../../../csw-database/src/main/resources/reference.conf)

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

By default csw configures `HikariCP` connection pool for managing connections with PostgreSQL server. To know more about `HikariCP`
please refer this [link](http://brettwooldridge.github.io/HikariCP/). 

@@@  

## Using DSLContext

Once the DSLContext is returned from `makeDsl/jMakeDsl`, it can be used to provide plain SQL to Database Service and 
get it executed on the PostgreSQL server.

The following sections show examples of most typical SQL use cases.

### Create

To create a table, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-create }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-create }

### Insert

To insert data in batch, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-batch }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-batch }

@@@note

* The insert statements above gets mapped to prepared statements underneath at JDBC layer and values like `movie_1`,
 `movie_2` and `2` from the example are bound to the dynamic parameters of these generated prepared statements.
* As prepared statements provide safety against SQL injection, it is recommended to use prepared statements instead of static
 SQL statements whenever there is a need to dynamically bind values.
* In the above example, two insert statements are batched together and sent to postgres server in a single call. 
 `executeBatchAsync/executeBatch` maps to batch statements underneath at JDBC layer.

@@@

### Select

To select data from table, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-fetch }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-fetch }


@@@note

Make sure that variable name and type of Films class is same as column's name and type in database. This is necessary for 
successful mapping of table fields to domain model class.

@@@


### Stored Function

To create a stored function, use the DSLContext as follows:

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dsl-function }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/database/JAssemblyComponentHandlers.java) { #dsl-function }

Similarly, any SQL queries can be written with the help of DSLContext including stored procedures.

@@@note

If there is a syntax error in SQL queries the `Future/CompletableFuture` returned will fail with `CompletionException` and 
`CompletionStage` will fail with `ExecutionException`. But both `CompletionException` and `ExecutionException` will have 
Jooq's `DataAccessException` underneath as cause. 

@@@

These examples are just a start. Any SQL statements can be created and executed using the DSLContext.

## Source code for examples

* @github[Scala Example](/examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala)
* @github[Java Example](/examples/src/main/java/csw/database/JAssemblyComponentHandlers.java)

