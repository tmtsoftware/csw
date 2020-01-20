# Database Service

## Introduction

The Database Service is included in TMT Common Software for use by components that need the features of a relational database. 
CSW Database Service provides a TMT-standard relational database and connection library. Databases created by Database Service 
will be stored reliably at the site during operations.

Database Service provides access to the TMT-standard PostgeSQL database server. It is a minimal service focused on providing
access to the database server through Location Service and standardizing on access libraries and drivers. The user documentation
@ref[here](../../services/database.md) provides much of what is needed to understand the Database Service.

### Code Outline

The Database Service code exists in the csw-database project within the CSW source code repository. Database Service consists of 8 files in 4 
packages written entirely in Scala. The Database Service depends on the PostgreSQL driver and the Jooq library.
 
The following table provides an overview of the packages and files in the Database Service with links to the source code.

| Package  | File  | Description |
|---:|:---|:---|
| `commons`  |  [DatabaseLogger](/csw-database/src/main/scala/csw/database/commons/DatabaseLogger.scala)  | An internal class to allow Database Service to log to Logging Service |
| `commons`  | [DatabaseServiceConnection](/csw-database/src/main/scala/csw/database/commons/DatabaseServiceConnection.scala)  | `DatabaseServiceConnection` is a wrapper over predefined `TcpConnection` representing Database Service. It is used to resolve the Database Service location.|
| `commons`  | [DatabaseServiceLocationResolver](/csw-database/src/main/scala/csw/database/commons/DatabaseServiceLocationResolver.scala)  | Provides the connection information of `Database Service` by resolving the location. |
| `exception`  | [DatabaseException](/csw-database/src/main/scala/csw/database/exceptions/DatabaseException.scala) | Represents an exception while connecting to the database server e.g. in case of providing incorrect username or password. |
| `javadsl`  | [AsyncHelper](/csw-database/src/main/scala/csw/database/javadsl/AsyncHelper.scala) |  A Java helper to schedule and execute blocking operations on a dedicated thread pool. This mechanism will prevent any blocking operation to be scheduled on a thread pool designated for async operations.|
| `javadsl`  |  [JooqHelper](/csw-database/src/main/scala/csw/database/javadsl/JooqHelper.scala)| A Java helper wrapping some of the Jooq operations.|
| `scaladsl`  | [JooqExtensions](/csw-database/src/main/scala/csw/database/scaladsl/JooqExtentions.scala)  | A Scala extension, extending few of the Jooq operations. |
|   | [DatabaseServiceFactory](/csw-database/src/main/scala/csw/database/DatabaseServiceFactory.scala)  |  DatabaseServiceFactory provides a mechanism to connect to the database server and get the handle of Jooq's DSLContext.|

For all other Database Service information see the @ref[user documentation](../../services/database.md).