# Native Auth Adapter (csw-aas-native)

csw-aas-native is the adapter you will use if you want to build an that executes on user's 
machine & talks to auth-protected web service application. Examples of such applications 
could be a CLI app that is installed on end users machine.

## Dependencies

To use the Akka HTTP Adapter (csw-aas-native), add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-aas-native" % "$version$"
    ```
    @@@
 
 
## Getting Started

Start csw services using csw-services.sh script. It will start AAS server and 
register with location service.

## Building a CLI Application

Let's say that we have an existing akka-http application which has some open and 
some protected routes and we want to build a CLI client which accesses these routes.

This is what the routes look like:

Scala
:   @@snip [Routes](../../../../../examples/src/main/scala/csw/auth/native/SampleRoutes.scala) { #sample-routes }


Note: To know more about how to create secure web apis, 
please go through @ref:[Akka HTTP Adapter - csw-aas-http](csw-aas-http.md)

We want to create a CLI app that has following commands/options:

```bash
Usage

 demo-cli [options] command [command options]

Commands

   login [command options] : performs user authentication
      --console : instead of using browser, prompts credentials on console

   logout : logs user out

   read : reads data from server

   write <content> : writes data to server
```

Although this example uses a library called [clist](https://github.com/backuity/clist) for 
argument parsing and command invocation, you can chose to use any other library for this purpose. 

First, thing will do is create an instance of NativeAppAuthAdapter. There is a factory already available to create the 
required instance. We will create a small factory on top of it.

Scala
:   @@snip [Adapter-Factory](../../../../../examples/src/main/scala/csw/auth/native/AdapterFactory.scala) { #adapter-factory }

Note the the internal factory overload we have used, requires two parameters, i.e. location service & authStore.
It needs location service to resolve keycloak. FileAuthStore is just a storage for tokens for it to 
save access tokens & refresh tokens. In this case we have configured it to store all tokens in 
"/tmp/demo-cli/auth" directory but ideally you want this location to be somewhere in user's home directory.
This will ensure that different users don't have access to each other's tokens.

Here's how the Main.scala looks like:

Scala
:   @@snip [Main](../../../../../examples/src/main/scala/csw/auth/native/Main.scala) { #main-app }

The statement `LocationServerStatus.requireUpLocally()` ensures that location service is up and running
before proceeding further. If location service is not running, it will throw an exception and exit the 
application.

//todo: keycloak setup
//todo: application.conf