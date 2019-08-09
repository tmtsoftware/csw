# Installed Auth Adapter (csw-aas-installed)

`csw-aas-installed` is the adapter you will use if you want to build a client application that executes on a user's 
machine and talks to an AAS-protected web service application, such as a CLI application.
The @ref:[Configuration Service Admin API](../../services/config.md) makes use of this library.

This is as opposed to building a web application that runs in a browser.  To do that, use the 
@ref:[csw-aas-http](csw-aas-http.md) library.

## Dependencies

To use the Akka HTTP Adapter (csw-aas-installed), add the following to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-aas-installed" % "$version$"
    ```
    @@@

## Prerequisites

To run a client app with AAS access, we need

* The CSW Location Service running
* An AAS instance running and registered with the Location Service
* A protected HTTP server running

All of these can be running on different machines. To start a Location Service and AAS 
server on a local machine, you can make use of the`csw-services.sh` script.

## Application Configurations

All AAS related configurations go inside an `auth-config` block in your `application.conf` file. There are two configurations 
applicable for a public client application: `realm` and `client-id`.

`realm` has a
default value of `TMT` if not specified. Normally, all apps in TMT should not have to override
this, however it might be useful to override this while testing your app.

`client-id` is a mandatory configuration which specifies the client ID of the app as per its registration
in AAS.

```hocon
auth-config {
  realm = TMT # DEFAULT
  client-id = demo-cli # REQUIRED
}
```

## Building a CLI Application

Let's say that we have an existing Akka HTTP application which has some open and 
some protected routes, and we want to build a CLI client which accesses these routes.

Scala
:   @@snip [Routes](../../../../../examples/src/main/scala/example/auth/installed/SampleRoutes.scala) { #sample-routes }

@@@ note
To know more about how to create secure web APIs, please go through 
@ref:[Akka HTTP Adapter - csw-aas-http](csw-aas-http.md)
@@@

We will create a CLI application that has following commands:

| command         | description                  |
| :-------------- | :--------------------------- |
| login           | performs user authentication |
| logout          | logs user out                |
| read            | reads data from server       |
| write {content} | writes data to server        |

Let's begin with `Main.scala`

Scala
:   @@snip [Main](../../../../../examples/src/main/scala/example/auth/installed/Main.scala) { #main-app }

The statement `LocationServerStatus.requireUpLocally()` ensures that the Location Service is up and running
before proceeding further. If it is not running, an exception will be thrown and the application will exit.

@@@ note
In a real application, you would ideally want to use `LocationServerStatus.requireUp` which takes
`locationHost: String` parameter instead of looking for the Location Service on the localhost. 
@@@

Next, we will instantiate `InstalledAppAuthAdapter`. There is a factory already available to create the 
required instance. We will create a small factory on top of this factory to keep our Main.scala clean.

Scala
:   @@snip [Adapter-Factory](../../../../../examples/src/main/scala/example/auth/installed/AdapterFactory.scala) { #adapter-factory }

Note the the internal factory method we have used requires two parameters: a reference to the Location Service
to resolve the AAS Server, and `authStore`, which is a file-based access token storage system.


@@@ warning { title='Warning' }
In this case we have configured it to store all tokens in the "/tmp/demo-cli/auth" 
directory, but ideally you want this location to be somewhere in the user's home directory.
This will ensure that different users don't have access to each other's tokens.
@@@

Coming back to Main.scala, now we need to find out which command the user wants to execute. To parse the
user input arguments, we will create a small utility.

Scala
:   @@snip [Command-Factory](../../../../../examples/src/main/scala/example/auth/installed/commands/CommandFactory.scala) { #command-factory }
 
All of these commands extend from a simple trait - `AppCommand`.

Scala
:   @@snip [AppCommand](../../../../../examples/src/main/scala/example/auth/installed/commands/AppCommand.scala) { #app-command }


Its single method `run` is executed in our application once the arguments are parsed into an `AppCommand`.

@@@ note
We could have used a command line parser library here to parse the command names and options/arguments, but since 
our requirements are simple and this is a demonstration, we will keep things simple. However, we 
strongly recommend that you use one of the existing libraries. CSW makes extensive use of 
[scopt](https://github.com/scopt/scopt). There are other libraries which are equally good and easy to use.

@@@

Let's go through each command one by one:

### Login

Scala
:   @@snip [LoginCommand](../../../../../examples/src/main/scala/example/auth/installed/commands/LoginCommand.scala) { #login-command }

Here the constructor takes an `InstalledAppAuthAdapter` as a parameter, and in the `run` method, 
it calls `installedAppAuthAdapter.login()`. This method opens a browser and redirects the user
to a TMT login screen (served by AAS). In the background, it starts an HTTP server
on a random port. Once the user submits the correct credentials on the login screen, AAS
redirects the user to `http://localhost:[SomePort]` with the access and refresh tokens in a
query string. The `InstalledAppAuthAdapter` will then save these tokens to the file system using 
`FileAuthStore`. After this, `InstalledAppAuthAdapter` will shut down the local server since it is
no longer needed. The user can then close the browser.

If you want to develop a CLI app that is not dependent on a browser, you can call
`loginCommandLine()` method instead of `login()`. This will prompt the user to provide credentials in the CLI 
instead of opening a browser.

@@@ note 
While the `loginCommandLine()` method is available, a browser is generally more
user-friendly since it can store cookies and remember passwords.
@@@

### Logout

Scala
:   @@snip [LogoutCommand](../../../../../examples/src/main/scala/example/auth/installed/commands/LogoutCommand.scala) { #logout-command }

The structure here is very similar to the `login` command. `installedAppAuthAdapter.logout()` 
clears all the tokens from the file system via `FileAuthStore`.

### Read

Scala
:   @@snip [ReadCommand](../../../../../examples/src/main/scala/example/auth/installed/commands/ReadCommand.scala) { #read-command }

Since the get route is not protected by any authentication or
authorization in the our example server, the `read` command simply sends a get request and prints the response.

### Write

Scala
:   @@snip [WriteCommand](../../../../../examples/src/main/scala/example/auth/installed/commands/WriteCommand.scala) { #write-command }

The WriteCommand constructor takes an `InstalledAppAuthAdapter` and a string value, passed in at the command line.
Since the post route is protected by a realm role policy in our example server, we need to pass a
bearer token in the request header. 

`installedAppAuthAdapter.getAccessTokenString()` checks the `FileAuthStore` and returns an `Option[String]`.
If the Option is `None`, it means that user has not logged in and an error message is displayed. If the token is 
found, the bearer token is obtained and passed in the header of the request to the HTTP server.  The HTTP server 
uses this token to determine whether the client has the proper permissions to perform the request.

If the response status code is 200, it means authentication and authorization were successful. In our example,
authorization required that the user had the `admin` role. 

If the response is 401 (`StatusCodes.Unauthorized`), there is something wrong with the token. 
It could indicate that token has expired or does not have a valid signature. 
`InstalledAppAuthAdapter` ensures that you don't send a request with an expired token.
If the access token is expired, it refreshes the access token with the help of a `refresh` token.
If the refresh token has also expired, it returns `None` which means that user has to log in again.

If the response is 403 (`StatusCodes.Forbidden`), the token is valid but the token is not authorized to 
perform that action. In our example, this would occur if the user does not have the `admin` role.

## Source code for above examples

@github[Example](/examples/src/main/scala/example/auth/installed)
